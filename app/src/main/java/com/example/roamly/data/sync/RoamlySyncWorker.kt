
package com.example.roamly.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.roamly.data.auth.RoamlySession
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineHolidayEntity
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RoamlySyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val database =
        RoamlyOfflineDatabase.getInstance(applicationContext)

    private val holidays = database.holidayDao()
    private val records = database.travelRecordDao()
    private val sessions = SessionManager(applicationContext)
    private val gson = Gson()

    private val api = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(SyncApiService::class.java)

    override suspend fun doWork(): Result = syncMutex.withLock {
        execute()
    }

    private suspend fun execute(): Result {
        val session = sessions.getSession() ?: return Result.success()
        val auth = "Bearer ${session.accessToken}"
        var failed = false

        return try {
            val check = api.checkDatabase()

            if (!check.isSuccessful) {
                Log.e(TAG, "Azure unavailable: HTTP ${check.code()}")
                return Result.retry()
            }

            try {
                syncHolidays(session, auth)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failed = true
                Log.e(TAG, "Holiday synchronization failed", error)
            }

            try {
                if (!HolidayCoverSync(applicationContext).uploadPending()) {
                    failed = true
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failed = true
                Log.e(
                    TAG,
                    "Holiday cover synchronization failed",
                    error
                )
            }


            try {
                val travel =
                    TravelSyncUploader(applicationContext).uploadPending()

                if (!travel.success) {
                    failed = true
                    Log.e(
                        TAG,
                        "Travel synchronization incomplete: ${travel.message}"
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failed = true
                Log.e(TAG, "Travel synchronization failed", error)
            }

            try {
                restoreFromAzure(session, auth)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failed = true
                Log.e(TAG, "Cloud restoration failed", error)
            }

            if (failed) Result.retry() else Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Synchronization failed", error)
            Result.retry()
        }
    }

    private suspend fun syncHolidays(
        session: RoamlySession,
        auth: String
    ) {
        val userId = session.userId
        val mappings = HolidayIdMappingStore(applicationContext)
        val remote =
            readArray(api.getHolidays(auth), "holidays").toMutableList()

        var failures = 0

        for (snapshot in holidays.getHolidays(userId).filter { it.needsSync }) {
            if (sessions.getSession()?.userId != userId) return

            try {
                val local =
                    gson.fromJson(snapshot.holidayJson, Holiday::class.java)

                val body = JsonObject().apply {
                    addProperty("title", local.name.trim())
                    addProperty("tripType", local.tripType.trim())
                    addProperty("country", local.country.trim())
                    addProperty("city", local.city.trim())
                    addProperty("startDate", local.startDate)
                    addProperty("endDate", local.endDate)

                    local.coverImageUri
                        ?.takeIf { it.startsWith("https://") }
                        ?.let { addProperty("imageUrl", it) }
                }

                var serverId = mappings.getServerId(snapshot.holidayId)
                val attemptId = snapshot.holidayId.toString()

                val attempt = records.getRecordById(
                    userId,
                    "holiday_create_attempt",
                    attemptId
                )

                if (serverId == null && attempt != null) {
                    val expected = gson.fromJson(
                        attempt.dataJson,
                        JsonObject::class.java
                    )

                    val matches = remote.filter {
                        sameHoliday(it, expected)
                    }

                    if (matches.size == 1) {
                        val matchedId =
                            text(matches.single(), "holidayId")
                                ?: error("Matched Azure holiday has no ID")

                        database.withTransaction {
                            mappings.saveMapping(
                                snapshot.holidayId,
                                matchedId
                            )

                            records.deleteRecord(
                                userId,
                                "holiday_create_attempt",
                                attemptId
                            )
                        }

                        serverId = matchedId
                    } else if (matches.size > 1) {
                        error(
                            "Multiple matching Azure holidays for local holiday ${snapshot.holidayId}"
                        )
                    } else if (
                        System.currentTimeMillis() -
                        attempt.updatedAtMillis < 120000L
                    ) {
                        error(
                            "Waiting to verify previous upload attempt for ${snapshot.holidayId}"
                        )
                    } else {
                        records.deleteRecord(
                            userId,
                            "holiday_create_attempt",
                            attemptId
                        )
                    }
                }

                if (serverId == null) {
                    records.saveRecord(
                        OfflineTravelRecordEntity(
                            userId = userId,
                            recordType = "holiday_create_attempt",
                            recordId = attemptId,
                            dataJson = body.toString(),
                            needsSync = false
                        )
                    )

                    val created = api.createHoliday(auth, body)

                    if (!created.isSuccessful) {
                        if (
                            created.code() in 400..499 &&
                            created.code() !in listOf(408, 409, 429)
                        ) {
                            records.deleteRecord(
                                userId,
                                "holiday_create_attempt",
                                attemptId
                            )
                        }

                        error(
                            "Azure holiday creation failed: HTTP ${created.code()} ${
                                created.errorBody()?.string().orEmpty().take(250)
                            }"
                        )
                    }

                    val returned =
                        created.body()?.getAsJsonObject("holiday")
                            ?: error("Azure did not return a holiday")

                    val createdId =
                        text(returned, "holidayId")
                            ?: error("Azure did not return a holiday ID")

                    database.withTransaction {
                        mappings.saveMapping(
                            snapshot.holidayId,
                            createdId
                        )

                        records.deleteRecord(
                            userId,
                            "holiday_create_attempt",
                            attemptId
                        )
                    }

                    serverId = createdId
                    remote.add(returned)
                } else {
                    val patched = api.updateHoliday(
                        auth,
                        serverId,
                        body
                    )

                    if (!patched.isSuccessful) {
                        error(
                            "Azure holiday update failed: HTTP ${patched.code()} ${
                                patched.errorBody()?.string().orEmpty().take(250)
                            }"
                        )
                    }
                }

                database.withTransaction {
                    val current = holidays.getHolidayById(
                        userId,
                        snapshot.holidayId
                    )

                    if (
                        current != null &&
                        current.needsSync &&
                        current.holidayJson == snapshot.holidayJson &&
                        current.updatedAtMillis == snapshot.updatedAtMillis
                    ) {
                        holidays.saveHoliday(
                            current.copy(needsSync = false)
                        )
                    }
                }

                Log.i(
                    TAG,
                    "Uploaded holiday ${snapshot.holidayId}"
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures++
                Log.e(
                    TAG,
                    "Unable to upload holiday ${snapshot.holidayId}",
                    error
                )
            }
        }

        val deletions = records.getRecords(
            userId,
            "holiday_deletion"
        ).filter { it.needsSync }

        for (deletion in deletions) {
            try {
                val localId = deletion.recordId.toInt()
                val serverId = mappings.getServerId(localId)

                val attempt = records.getRecordById(
                    userId,
                    "holiday_create_attempt",
                    deletion.recordId
                )

                if (serverId == null && attempt != null) {
                    error(
                        "Holiday $localId deletion is waiting for creation reconciliation"
                    )
                }

                if (serverId != null) {
                    val response = api.deleteHoliday(auth, serverId)

                    if (
                        !response.isSuccessful &&
                        response.code() != 404
                    ) {
                        error(
                            "Azure holiday deletion failed: HTTP ${response.code()}"
                        )
                    }
                }

                database.withTransaction {
                    val current = records.getRecordById(
                        userId,
                        "holiday_deletion",
                        deletion.recordId
                    )

                    if (
                        current != null &&
                        current.dataJson == deletion.dataJson &&
                        current.updatedAtMillis == deletion.updatedAtMillis
                    ) {
                        records.deleteRecord(
                            userId,
                            "holiday_deletion",
                            deletion.recordId
                        )

                        records.deleteRecord(
                            userId,
                            "holiday_server_mapping",
                            deletion.recordId
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures++
                Log.e(
                    TAG,
                    "Unable to delete holiday ${deletion.recordId}",
                    error
                )
            }
        }

        if (failures > 0) {
            error("$failures holiday changes remain pending")
        }
    }

    private suspend fun restoreFromAzure(
        session: RoamlySession,
        auth: String
    ) {
        val userId = session.userId

        if (sessions.getSession()?.userId != userId) return
        if (holidays.getHolidays(userId).any { it.needsSync }) return

        val mappings = HolidayIdMappingStore(applicationContext)
        val remoteHolidays =
            readArray(api.getHolidays(auth), "holidays")

        val localHolidays = holidays.getHolidays(userId)

        val existingMappingRecords = records.getRecords(
            userId,
            "holiday_server_mapping"
        )

        val idByServerId = existingMappingRecords.mapNotNull { record ->
            val id = runCatching {
                text(
                    gson.fromJson(
                        record.dataJson,
                        JsonObject::class.java
                    ),
                    "serverHolidayId"
                )
            }.getOrNull()

            if (id == null) {
                null
            } else {
                id to record.recordId.toInt()
            }
        }.toMap().toMutableMap()

        var nextId = (
                localHolidays.map { it.holidayId } +
                        existingMappingRecords.mapNotNull {
                            it.recordId.toIntOrNull()
                        } +
                        records.getRecords(
                            userId,
                            "holiday_deletion"
                        ).mapNotNull {
                            it.recordId.toIntOrNull()
                        }
                ).maxOrNull() ?: 0

        for (remote in remoteHolidays) {
            if (sessions.getSession()?.userId != userId) return

            val serverId =
                text(remote, "holidayId") ?: continue

            var localId = idByServerId[serverId]

            if (localId == null) {
                nextId++
                localId = nextId

                mappings.saveMapping(
                    localId,
                    serverId
                )

                idByServerId[serverId] = localId
            }

            if (
                records.getRecordById(
                    userId,
                    "holiday_deletion",
                    localId.toString()
                ) != null
            ) {
                continue
            }

            val current = holidays.getHolidayById(
                userId,
                localId
            )

            if (current?.needsSync == true) continue

            if (current == null) {
                val city = text(remote, "city").orEmpty()
                val country = text(remote, "country").orEmpty()

                val holiday = Holiday(
                    holidayId = localId,
                    name = text(remote, "title").orEmpty(),
                    location = "$city, $country".trim(' ', ','),
                    startDate = text(remote, "startDate")
                        .orEmpty().take(10),
                    endDate = text(remote, "endDate")
                        .orEmpty().take(10),
                    tripType = text(remote, "tripType").orEmpty(),
                    city = city,
                    country = country,
                    coverImageUri = text(remote, "imageUrl")
                )

                holidays.saveHoliday(
                    OfflineHolidayEntity(
                        userId = userId,
                        holidayId = localId,
                        name = holiday.name,
                        location = holiday.location,
                        startDate = holiday.startDate,
                        endDate = holiday.endDate,
                        holidayJson = gson.toJson(holiday),
                        needsSync = false
                    )
                )


                Log.i(TAG, "Restored holiday $localId")
            } else {
                val imageUrl = text(remote, "imageUrl")

                if (!imageUrl.isNullOrBlank()) {
                    val currentHoliday = gson.fromJson(
                        current.holidayJson,
                        Holiday::class.java
                    )

                    if (currentHoliday.coverImageUri.isNullOrBlank()) {
                        holidays.saveHoliday(
                            current.copy(
                                holidayJson = gson.toJson(
                                    currentHoliday.copy(
                                        coverImageUri = imageUrl
                                    )
                                ),
                                needsSync = false
                            )
                        )
                    }
                }
            }

        }

        val itineraries = readArray(
            api.getItineraries(auth),
            "itineraries"
        )

        for (itinerary in itineraries) {
            if (sessions.getSession()?.userId != userId) return

            val serverHolidayId =
                text(itinerary, "holidayId") ?: continue

            val localHolidayId =
                idByServerId[serverHolidayId] ?: continue

            if (
                holidays.getHolidayById(
                    userId,
                    localHolidayId
                ) == null
            ) {
                continue
            }

            val days = array(itinerary, "days")

            for (day in days) {
                val dayNumber =
                    day.get("dayNumber")?.asInt ?: continue

                val dayId = "$localHolidayId:$dayNumber"

                val pendingDeletion = records.getRecordById(
                    userId,
                    "itinerary_day_deletion",
                    dayId
                )

                if (pendingDeletion != null) continue

                if (
                    records.getRecordById(
                        userId,
                        "itinerary_day",
                        dayId
                    ) == null
                ) {
                    val localDay = ItineraryDay(
                        dayNumber,
                        text(day, "date").orEmpty().take(10)
                    )

                    records.saveRecord(
                        OfflineTravelRecordEntity(
                            userId = userId,
                            recordType = "itinerary_day",
                            recordId = dayId,
                            dataJson = gson.toJson(localDay),
                            needsSync = false
                        )
                    )
                }

                val activities = array(day, "activities")

                for (activity in activities) {
                    val serverActivityId =
                        text(activity, "activityId") ?: continue

                    val existingMappings = records.getRecords(
                        userId,
                        "sync_mapping_itinerary_activity"
                    )

                    val existingLocalId = existingMappings.firstOrNull {
                        text(
                            gson.fromJson(
                                it.dataJson,
                                JsonObject::class.java
                            ),
                            "serverId"
                        ) == serverActivityId
                    }?.recordId

                    val recordId =
                        if (existingLocalId != null) {
                            existingLocalId
                        } else {
                            val used = records.getRecords(
                                userId,
                                "itinerary_activity"
                            ).filter {
                                it.recordId.startsWith("$localHolidayId:")
                            }.mapNotNull {
                                it.recordId.substringAfter(':')
                                    .toIntOrNull()
                            }

                            val nextActivity =
                                (used.maxOrNull() ?: 0) + 1

                            "$localHolidayId:$nextActivity"
                        }

                    if (
                        records.getRecordById(
                            userId,
                            "itinerary_activity_deletion",
                            recordId
                        ) != null
                    ) {
                        continue
                    }

                    if (
                        records.getRecordById(
                            userId,
                            "itinerary_activity",
                            recordId
                        ) == null
                    ) {
                        val localActivity = ItineraryActivity(
                            activityId = recordId.substringAfter(':').toInt(),
                            dayNumber = dayNumber,
                            title = text(activity, "title").orEmpty(),
                            startTime = time(
                                activity,
                                "startTime"
                            ).orEmpty(),
                            endTime = time(activity, "endTime"),
                            location = text(activity, "location"),
                            notes = text(activity, "notes")
                        )

                        database.withTransaction {
                            records.saveRecord(
                                OfflineTravelRecordEntity(
                                    userId = userId,
                                    recordType = "itinerary_activity",
                                    recordId = recordId,
                                    dataJson = gson.toJson(localActivity),
                                    needsSync = false
                                )
                            )

                            records.saveRecord(
                                OfflineTravelRecordEntity(
                                    userId = userId,
                                    recordType =
                                        "sync_mapping_itinerary_activity",
                                    recordId = recordId,
                                    dataJson = JsonObject().apply {
                                        addProperty(
                                            "serverId",
                                            serverActivityId
                                        )
                                    }.toString(),
                                    needsSync = false
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sameHoliday(
        remote: JsonObject,
        expected: JsonObject
    ): Boolean {
        val fields = listOf(
            "title",
            "tripType",
            "country",
            "city"
        )

        return fields.all {
            text(remote, it) == text(expected, it)
        } &&
                text(remote, "startDate")?.take(10) ==
                text(expected, "startDate")?.take(10) &&
                text(remote, "endDate")?.take(10) ==
                text(expected, "endDate")?.take(10)
    }

    private fun readArray(
        response: Response<JsonObject>,
        key: String
    ): List<JsonObject> {
        if (!response.isSuccessful) {
            error("Azure request failed: HTTP ${response.code()}")
        }

        return array(
            response.body()
                ?: error("Azure returned an empty response"),
            key
        )
    }

    private fun array(
        json: JsonObject,
        key: String
    ): List<JsonObject> =
        json.get(key)
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.mapNotNull {
                if (it.isJsonObject) it.asJsonObject else null
            }
            ?: emptyList()

    private fun text(
        json: JsonObject,
        key: String
    ): String? =
        json.get(key)
            ?.takeUnless { it.isJsonNull }
            ?.asString

    private fun time(
        json: JsonObject,
        key: String
    ): String? {
        val raw = text(json, key) ?: return null

        return if ('T' in raw) {
            raw.substringAfter('T').take(5)
        } else {
            raw.take(5)
        }
    }

    companion object {
        private const val TAG = "RoamlySync"
        private val syncMutex = Mutex()
    }
}
