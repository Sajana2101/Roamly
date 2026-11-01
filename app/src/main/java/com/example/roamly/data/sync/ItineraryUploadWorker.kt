
package com.example.roamly.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ItineraryUploadWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val database =
        RoamlyOfflineDatabase.getInstance(applicationContext)
    private val dao = database.travelRecordDao()
    private val holidays = database.holidayDao()
    private val sessions = SessionManager(applicationContext)
    private val gson = Gson()

    private val api = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TravelSyncApi::class.java)

    override suspend fun doWork(): Result = mutex.withLock {
        val session = sessions.getSession()
            ?: return@withLock Result.success()

        val userId = session.userId
        val auth = "Bearer ${session.accessToken}"

        try {
            val pending = dao.getPendingRecords(userId).filter {
                it.recordType == "itinerary_day" ||
                        it.recordType == "itinerary_activity"
            }

            if (pending.isEmpty()) {
                return@withLock Result.success()
            }

            val holidayIds = pending.mapNotNull {
                it.recordId.substringBefore(':').toIntOrNull()
            }.distinct()

            val holidayMappings =
                HolidayIdMappingStore(applicationContext)

            for (localHolidayId in holidayIds) {
                check(sessions.getSession()?.userId == userId) {
                    "Signed-in user changed during itinerary upload."
                }

                val holiday = holidays.getHolidayById(
                    userId,
                    localHolidayId
                ) ?: error("Local holiday $localHolidayId is missing.")

                val serverHolidayId =
                    holidayMappings.getServerId(localHolidayId)
                        ?: error("Holiday $localHolidayId has not uploaded yet.")

                val startMillis = dateMillis(holiday.startDate)
                val endMillis = dateMillis(holiday.endDate)
                val prefix = "$localHolidayId:"

                val localDays = dao.getRecords(
                    userId,
                    "itinerary_day"
                ).filter {
                    it.recordId.startsWith(prefix)
                }

                val localActivities = dao.getRecords(
                    userId,
                    "itinerary_activity"
                ).filter {
                    it.recordId.startsWith(prefix) && it.needsSync
                }

                val serverItinerary =
                    getOrCreateItinerary(serverHolidayId, auth)

                val itineraryId = field(
                    serverItinerary,
                    "itineraryId"
                ) ?: error("Azure itinerary ID is missing.")

                val remoteDays = jsonObjects(
                    serverItinerary,
                    "days"
                ).toMutableList()

                val daysByLocalNumber = localDays.associateBy {
                    gson.fromJson(
                        it.dataJson,
                        ItineraryDay::class.java
                    ).dayNumber
                }

                for (record in localDays.filter { it.needsSync }) {
                    val day = gson.fromJson(
                        record.dataJson,
                        ItineraryDay::class.java
                    )

                    val date = normalizeDate(day.date)
                    val dayMillis = dateMillis(date)

                    require(dayMillis in startMillis..endMillis) {
                        "Itinerary day date $date is outside the holiday dates."
                    }

                    val serverDayNumber =
                        ((dayMillis - startMillis) / 86400000L).toInt() + 1

                    require(serverDayNumber in 1..366) {
                        "Itinerary day number is outside the server range."
                    }

                    val existing = remoteDays.firstOrNull {
                        field(it, "date")?.take(10) == date ||
                                it.get("dayNumber")?.asInt == serverDayNumber
                    }

                    val serverDay = if (existing != null) {
                        val serverId = field(
                            existing,
                            "itineraryDayId"
                        ) ?: error("Azure day ID is missing.")

                        if (
                            field(existing, "date")?.take(10) != date ||
                            existing.get("dayNumber")?.asInt != serverDayNumber
                        ) {
                            val response = api.patch(
                                "api/itineraries/days/$serverId",
                                auth,
                                dayBody(serverDayNumber, date)
                            )

                            requireSuccess(
                                response,
                                "Update itinerary day"
                            )
                        }

                        existing
                    } else {
                        val response = api.post(
                            "api/itineraries/$itineraryId/days",
                            auth,
                            dayBody(serverDayNumber, date)
                        )

                        if (response.code() == 409) {
                            val refreshed =
                                getItinerary(serverHolidayId, auth)

                            val matched = jsonObjects(
                                refreshed,
                                "days"
                            ).firstOrNull {
                                field(it, "date")?.take(10) == date
                            } ?: error(
                                "Azure reported an itinerary day conflict."
                            )

                            remoteDays.add(matched)
                            matched
                        } else {
                            requireSuccess(
                                response,
                                "Create itinerary day"
                            )

                            val created = response.body()
                                ?.getAsJsonObject("day")
                                ?: error("Azure did not return the created day.")

                            remoteDays.add(created)
                            created
                        }
                    }

                    val serverId = field(
                        serverDay,
                        "itineraryDayId"
                    ) ?: error("Azure day ID is missing.")

                    database.withTransaction {
                        saveMapping(
                            userId,
                            "itinerary_day",
                            record.recordId,
                            serverId
                        )

                        acknowledge(userId, record)
                    }
                }

                for (record in localActivities) {
                    val activity = gson.fromJson(
                        record.dataJson,
                        ItineraryActivity::class.java
                    )

                    val localDay =
                        daysByLocalNumber[activity.dayNumber]
                            ?: error(
                                "Activity ${activity.title} has no matching local day."
                            )

                    val day = gson.fromJson(
                        localDay.dataJson,
                        ItineraryDay::class.java
                    )

                    val date = normalizeDate(day.date)

                    val serverDay = remoteDays.firstOrNull {
                        field(it, "date")?.take(10) == date
                    } ?: error(
                        "Activity ${activity.title} is waiting for its Azure itinerary day."
                    )

                    val serverDayId = field(
                        serverDay,
                        "itineraryDayId"
                    ) ?: error("Azure day ID is missing.")

                    val body = JsonObject().apply {
                        addProperty("title", activity.title)
                        addProperty(
                            "startTime",
                            activity.startTime.take(5)
                        )

                        activity.endTime?.let {
                            addProperty("endTime", it.take(5))
                        }

                        activity.location?.let {
                            addProperty("location", it)
                        }

                        activity.notes?.let {
                            addProperty("notes", it)
                        }
                    }

                    val mappedId = readMapping(
                        userId,
                        "itinerary_activity",
                        record.recordId
                    )

                    val serverActivityId: String

                    if (mappedId != null) {
                        requireSuccess(
                            api.patch(
                                "api/itineraries/activities/$mappedId",
                                auth,
                                body
                            ),
                            "Update itinerary activity"
                        )

                        serverActivityId = mappedId
                    } else {
                        val freshItinerary =
                            getItinerary(serverHolidayId, auth)

                        val freshDay = jsonObjects(
                            freshItinerary,
                            "days"
                        ).firstOrNull {
                            field(it, "itineraryDayId") == serverDayId
                        }

                        val claimedIds = dao.getRecords(
                            userId,
                            "sync_mapping_itinerary_activity"
                        ).filter {
                            it.recordId != record.recordId
                        }.mapNotNull {
                            field(
                                gson.fromJson(
                                    it.dataJson,
                                    JsonObject::class.java
                                ),
                                "serverId"
                            )
                        }.toSet()

                        val matches = freshDay?.let {
                            jsonObjects(it, "activities")
                        }.orEmpty().filter {
                            field(it, "activityId") !in claimedIds &&
                                    field(it, "title") == activity.title &&
                                    field(it, "startTime")?.let { time ->
                                        if ('T' in time) {
                                            time.substringAfter('T').take(5)
                                        } else {
                                            time.take(5)
                                        }
                                    } == activity.startTime.take(5)
                        }

                        check(matches.size <= 1) {
                            "Multiple matching Azure activities require review."
                        }

                        serverActivityId = if (matches.size == 1) {
                            field(
                                matches.single(),
                                "activityId"
                            ) ?: error("Azure activity ID is missing.")
                        } else {
                            val response = api.post(
                                "api/itineraries/days/$serverDayId/activities",
                                auth,
                                body
                            )

                            requireSuccess(
                                response,
                                "Create itinerary activity"
                            )

                            val created = response.body()
                                ?.getAsJsonObject("activity")
                                ?: error(
                                    "Azure did not return the created activity."
                                )

                            field(created, "activityId")
                                ?: error("Azure activity ID is missing.")
                        }
                    }

                    database.withTransaction {
                        saveMapping(
                            userId,
                            "itinerary_activity",
                            record.recordId,
                            serverActivityId
                        )

                        acknowledge(userId, record)

                        dao.deleteRecord(
                            userId,
                            "sync_attempt_itinerary_activity",
                            record.recordId
                        )
                    }
                }
            }

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(
                "RoamlyItinerarySync",
                "Itinerary upload failed",
                error
            )
            Result.retry()
        }
    }

    private suspend fun getOrCreateItinerary(
        serverHolidayId: String,
        auth: String
    ): JsonObject {
        val path = "api/itineraries/holiday/$serverHolidayId"
        val existing = api.get(path, auth)

        if (existing.isSuccessful) {
            return existing.body()
                ?.getAsJsonObject("itinerary")
                ?: error("Azure itinerary response was empty.")
        }

        check(existing.code() == 404) {
            "Get itinerary HTTP ${existing.code()}"
        }

        val body = JsonObject().apply {
            addProperty("holidayId", serverHolidayId)
            addProperty("name", "Holiday Itinerary")
        }

        val created = api.post(
            "api/itineraries",
            auth,
            body
        )

        check(created.isSuccessful || created.code() == 409) {
            "Create itinerary HTTP ${created.code()}"
        }

        return getItinerary(serverHolidayId, auth)
    }

    private suspend fun getItinerary(
        serverHolidayId: String,
        auth: String
    ): JsonObject {
        val response = api.get(
            "api/itineraries/holiday/$serverHolidayId",
            auth
        )

        requireSuccess(response, "Get itinerary")

        return response.body()
            ?.getAsJsonObject("itinerary")
            ?: error("Azure itinerary response was empty.")
    }

    private fun dayBody(
        dayNumber: Int,
        date: String
    ) = JsonObject().apply {
        addProperty("dayNumber", dayNumber)
        addProperty("date", date)
    }

    private fun normalizeDate(date: String): String {
        val normalized = date.trim().replace('/', '-')
        dateMillis(normalized)
        return normalized
    }

    private fun dateMillis(date: String): Long {
        val format = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return format.parse(date)?.time
            ?: error("Invalid itinerary date: $date")
    }

    private suspend fun readMapping(
        userId: String,
        type: String,
        recordId: String
    ): String? {
        val record = dao.getRecordById(
            userId,
            "sync_mapping_$type",
            recordId
        ) ?: return null

        val json = gson.fromJson(
            record.dataJson,
            JsonObject::class.java
        )

        return field(json, "serverId")
    }

    private suspend fun saveMapping(
        userId: String,
        type: String,
        recordId: String,
        serverId: String
    ) {
        dao.saveRecord(
            OfflineTravelRecordEntity(
                userId = userId,
                recordType = "sync_mapping_$type",
                recordId = recordId,
                dataJson = JsonObject().apply {
                    addProperty("serverId", serverId)
                }.toString(),
                needsSync = false
            )
        )
    }

    private suspend fun acknowledge(
        userId: String,
        snapshot: OfflineTravelRecordEntity
    ) {
        val current = dao.getRecordById(
            userId,
            snapshot.recordType,
            snapshot.recordId
        ) ?: return

        if (
            current.needsSync &&
            current.dataJson == snapshot.dataJson &&
            current.updatedAtMillis == snapshot.updatedAtMillis
        ) {
            dao.saveRecord(
                current.copy(needsSync = false)
            )
        }
    }

    private fun field(
        json: JsonObject?,
        key: String
    ): String? =
        json?.get(key)
            ?.takeUnless { it.isJsonNull }
            ?.asString

    private fun jsonObjects(
        json: JsonObject,
        key: String
    ): List<JsonObject> =
        json.get(key)
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.mapNotNull {
                if (it.isJsonObject) {
                    it.asJsonObject
                } else {
                    null
                }
            } ?: emptyList()

    private fun requireSuccess(
        response: Response<JsonObject>,
        action: String
    ) {
        check(response.isSuccessful) {
            "$action failed: HTTP ${response.code()} ${
                response.errorBody()?.string().orEmpty().take(200)
            }"
        }
    }

    companion object {
        private val mutex = Mutex()
    }
}
