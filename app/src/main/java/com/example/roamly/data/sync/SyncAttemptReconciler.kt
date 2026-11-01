
package com.example.roamly.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ReconciliationResult(
    val resolved: Int,
    val unresolved: Int
)

class SyncAttemptReconciler(
    context: Context
) {
    private val appContext = context.applicationContext
    private val sessions = SessionManager(appContext)
    private val database =
        RoamlyOfflineDatabase.getInstance(appContext)
    private val dao = database.travelRecordDao()
    private val gson = Gson()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val holidayApi =
        retrofit.create(SyncApiService::class.java)

    private val travelApi =
        retrofit.create(TravelSyncApi::class.java)

    suspend fun reconcile(): ReconciliationResult =
        withContext(Dispatchers.IO) {
            val session = sessions.getSession()
                ?: return@withContext ReconciliationResult(0, 0)

            val userId = session.userId
            val auth = "Bearer ${session.accessToken}"

            val types = listOf(
                "holiday_create_attempt",
                "sync_attempt_packing_list",
                "sync_attempt_packing_item",
                "sync_attempt_itinerary_activity"
            )

            val attempts = types.flatMap {
                dao.getRecords(userId, it)
            }

            if (attempts.isEmpty()) {
                return@withContext ReconciliationResult(0, 0)
            }

            val holidays =
                if (attempts.any {
                        it.recordType == "holiday_create_attempt"
                    }) {
                    val response = holidayApi.getHolidays(auth)

                    check(response.isSuccessful) {
                        "Unable to retrieve Azure holidays."
                    }

                    objects(response.body(), "holidays")
                } else {
                    emptyList()
                }

            val packing =
                if (attempts.any {
                        it.recordType.startsWith(
                            "sync_attempt_packing_"
                        )
                    }) {
                    val response = travelApi.get(
                        "api/packing",
                        auth
                    )

                    check(response.isSuccessful) {
                        "Unable to retrieve Azure packing records."
                    }

                    objects(response.body(), "packingLists")
                } else {
                    emptyList()
                }

            val itineraries =
                if (attempts.any {
                        it.recordType ==
                                "sync_attempt_itinerary_activity"
                    }) {
                    val response = travelApi.get(
                        "api/itineraries",
                        auth
                    )

                    check(response.isSuccessful) {
                        "Unable to retrieve Azure itineraries."
                    }

                    objects(response.body(), "itineraries")
                } else {
                    emptyList()
                }

            var resolved = 0

            for (attempt in attempts) {
                check(sessions.getSession()?.userId == userId) {
                    "The authenticated user changed."
                }

                val mappingType = when (attempt.recordType) {
                    "holiday_create_attempt" ->
                        "holiday_server_mapping"

                    else ->
                        "sync_mapping_" +
                                attempt.recordType.removePrefix(
                                    "sync_attempt_"
                                )
                }

                val existingMapping = dao.getRecordById(
                    userId,
                    mappingType,
                    attempt.recordId
                )

                val candidates =
                    if (existingMapping != null) {
                        emptyList()
                    } else {
                        findMatches(
                            userId,
                            attempt,
                            holidays,
                            packing,
                            itineraries
                        )
                    }

                if (
                    existingMapping == null &&
                    candidates.size != 1
                ) {
                    continue
                }

                val serverId =
                    candidates.singleOrNull()

                database.withTransaction {
                    val current = dao.getRecordById(
                        userId,
                        attempt.recordType,
                        attempt.recordId
                    )

                    if (
                        current != null &&
                        current.dataJson == attempt.dataJson &&
                        current.updatedAtMillis ==
                        attempt.updatedAtMillis
                    ) {
                        if (serverId != null) {
                            val validated =
                                UUID.fromString(serverId)
                                    .toString()

                            val mappingData =
                                JsonObject().apply {
                                    if (
                                        mappingType ==
                                        "holiday_server_mapping"
                                    ) {
                                        addProperty(
                                            "localHolidayId",
                                            attempt.recordId.toInt()
                                        )
                                        addProperty(
                                            "serverHolidayId",
                                            validated
                                        )
                                    } else {
                                        addProperty(
                                            "serverId",
                                            validated
                                        )
                                    }
                                }

                            dao.saveRecord(
                                OfflineTravelRecordEntity(
                                    userId = userId,
                                    recordType = mappingType,
                                    recordId = attempt.recordId,
                                    dataJson = mappingData.toString(),
                                    needsSync = false
                                )
                            )
                        }

                        dao.deleteRecord(
                            userId,
                            attempt.recordType,
                            attempt.recordId
                        )

                        resolved++
                    }
                }
            }

            ReconciliationResult(
                resolved = resolved,
                unresolved = attempts.size - resolved
            )
        }

    private suspend fun findMatches(
        userId: String,
        attempt: OfflineTravelRecordEntity,
        holidays: List<JsonObject>,
        packing: List<JsonObject>,
        itineraries: List<JsonObject>
    ): List<String> {
        val expected = gson.fromJson(
            attempt.dataJson,
            JsonObject::class.java
        )

        return when (attempt.recordType) {
            "holiday_create_attempt" -> {
                holidays.filter {
                    createdDuringAttempt(it, attempt) &&
                            sameFields(
                                expected,
                                it,
                                listOf(
                                    "title",
                                    "tripType",
                                    "country",
                                    "city"
                                )
                            ) &&
                            date(it, "startDate") ==
                            date(expected, "startDate") &&
                            date(it, "endDate") ==
                            date(expected, "endDate") &&
                            value(it, "imageUrl") ==
                            value(expected, "imageUrl")
                }.mapNotNull {
                    value(it, "holidayId")
                }
            }

            "sync_attempt_packing_list" -> {
                packing.filter {
                    createdDuringAttempt(it, attempt) &&
                            sameFields(
                                expected,
                                it,
                                listOf("name", "description")
                            )
                }.mapNotNull {
                    value(it, "packingListId")
                }
            }

            "sync_attempt_packing_item" -> {
                val local = dao.getRecordById(
                    userId,
                    "packing_item",
                    attempt.recordId
                ) ?: return emptyList()

                val item = gson.fromJson(
                    local.dataJson,
                    PackingItem::class.java
                )

                val mapping = dao.getRecordById(
                    userId,
                    "sync_mapping_packing_list",
                    item.packingListId
                ) ?: return emptyList()

                val serverListId = value(
                    gson.fromJson(
                        mapping.dataJson,
                        JsonObject::class.java
                    ),
                    "serverId"
                )

                packing.filter {
                    value(it, "packingListId") == serverListId
                }.flatMap {
                    objects(it, "items")
                }.filter {
                    createdDuringAttempt(it, attempt) &&
                            sameFields(
                                expected,
                                it,
                                listOf("name", "isPacked")
                            )
                }.mapNotNull {
                    value(it, "packingItemId")
                }
            }

            "sync_attempt_itinerary_activity" -> {
                val local = dao.getRecordById(
                    userId,
                    "itinerary_activity",
                    attempt.recordId
                ) ?: return emptyList()

                val activity = gson.fromJson(
                    local.dataJson,
                    ItineraryActivity::class.java
                )

                val localHolidayId =
                    attempt.recordId.substringBefore(":")
                        .toIntOrNull()
                        ?: return emptyList()

                val serverHolidayId =
                    HolidayIdMappingStore(appContext)
                        .getServerId(localHolidayId)
                        ?: return emptyList()

                itineraries.filter {
                    value(it, "holidayId") == serverHolidayId
                }.flatMap {
                    objects(it, "days")
                }.filter {
                    value(it, "dayNumber")?.toIntOrNull() ==
                            activity.dayNumber
                }.flatMap {
                    objects(it, "activities")
                }.filter {
                    createdDuringAttempt(it, attempt) &&
                            value(it, "title") ==
                            value(expected, "title") &&
                            time(it, "startTime") ==
                            time(expected, "startTime") &&
                            time(it, "endTime") ==
                            time(expected, "endTime") &&
                            value(it, "location") ==
                            value(expected, "location") &&
                            value(it, "notes") ==
                            value(expected, "notes")
                }.mapNotNull {
                    value(it, "activityId")
                }
            }

            else -> emptyList()
        }.distinct()
    }

    private fun createdDuringAttempt(
        remote: JsonObject,
        attempt: OfflineTravelRecordEntity
    ): Boolean {
        val text = value(remote, "createdAt")
            ?: return false

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )

        val timestamp = patterns.firstNotNullOfOrNull {
                pattern ->
            try {
                SimpleDateFormat(
                    pattern,
                    Locale.US
                ).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }.parse(text)?.time
            } catch (_: Exception) {
                null
            }
        } ?: return false

        return timestamp >=
                attempt.updatedAtMillis - 15000 &&
                timestamp <=
                attempt.updatedAtMillis + 300000
    }

    private fun sameFields(
        expected: JsonObject,
        actual: JsonObject,
        fields: List<String>
    ): Boolean {
        return fields.all {
            value(expected, it) == value(actual, it)
        }
    }

    private fun objects(
        json: JsonObject?,
        field: String
    ): List<JsonObject> {
        val array = json?.get(field)
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?: return emptyList()

        return array.mapNotNull {
            if (it.isJsonObject) it.asJsonObject else null
        }
    }

    private fun value(
        json: JsonObject?,
        field: String
    ): String? {
        val element = json?.get(field) ?: return null
        return if (element.isJsonNull) null else element.asString
    }

    private fun date(
        json: JsonObject?,
        field: String
    ): String? {
        return value(json, field)?.take(10)
    }

    private fun time(
        json: JsonObject?,
        field: String
    ): String? {
        val text = value(json, field) ?: return null

        return if ("T" in text) {
            text.substringAfter("T").take(5)
        } else {
            text.take(5)
        }
    }
}
