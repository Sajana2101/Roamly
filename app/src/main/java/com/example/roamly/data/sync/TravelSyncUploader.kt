
package com.example.roamly.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingList
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

data class TravelUploadResult(
    val uploaded: Int,
    val deleted: Int,
    val remaining: Int,
    val success: Boolean,
    val message: String
)

class TravelSyncUploader(
    context: Context
) {
    private val appContext = context.applicationContext
    private val sessions = SessionManager(appContext)
    private val database =
        RoamlyOfflineDatabase.getInstance(appContext)
    private val dao = database.travelRecordDao()
    private val gson = Gson()

    private val api = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create()
        )
        .build()
        .create(TravelSyncApi::class.java)

    private var uploaded = 0
    private var deleted = 0

    suspend fun uploadPending(): TravelUploadResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                uploaded = 0
                deleted = 0

                val session = sessions.getSession()
                    ?: return@withLock TravelUploadResult(
                        0, 0, 0, false, "Sign in required."
                    )

                val userId = session.userId
                val auth = "Bearer ${session.accessToken}"

                try {
                    val pending = pending(userId)

                    if (pending.isEmpty()) {
                        return@withLock TravelUploadResult(
                            0, 0, 0, true,
                            "No pending travel changes."
                        )
                    }

                    syncPacking(userId, auth)
                    syncItineraries(userId, auth)

                    val remaining = pending(userId).size

                    TravelUploadResult(
                        uploaded,
                        deleted,
                        remaining,
                        remaining == 0,
                        if (remaining == 0) {
                            "All travel changes uploaded."
                        } else {
                            "$remaining changes remain pending."
                        }
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    TravelUploadResult(
                        uploaded,
                        deleted,
                        pending(userId).size,
                        false,
                        error.message
                            ?: "Travel synchronization failed."
                    )
                }
            }
        }

    private suspend fun syncPacking(
        userId: String,
        auth: String
    ) {
        val remote = get(
            "api/packing",
            auth
        ).getAsJsonArray("packingLists")
            ?: throw IllegalStateException(
                "Azure packing response is invalid."
            )

        val lists = dao.getRecords(
            userId,
            "packing_list"
        ).filter { it.needsSync }

        for (record in lists) {
            ensureUser(userId)

            val list = gson.fromJson(
                record.dataJson,
                PackingList::class.java
            )

            var serverId = mapping(
                userId,
                "packing_list",
                record.recordId
            )

            if (serverId == null) {
                val existing = remote.firstOrNull {
                    string(
                        it.asJsonObject,
                        "packingListId"
                    ) == list.packingListId
                }

                if (existing != null) {
                    serverId = list.packingListId
                    saveMapping(
                        userId,
                        "packing_list",
                        record.recordId,
                        serverId
                    )
                }
            }

            val body = JsonObject().apply {
                addProperty("name", list.name)
                if (list.description != null) {
                    addProperty(
                        "description",
                        list.description
                    )
                }
            }

            if (serverId == null) {
                createMapped(
                    userId,
                    record,
                    "packing_list",
                    "api/packing",
                    "packingList",
                    "packingListId",
                    body,
                    auth
                )
            } else {
                updateMapped(
                    userId,
                    record,
                    "api/packing/$serverId",
                    body,
                    auth
                )
            }
        }

        val items = dao.getRecords(
            userId,
            "packing_item"
        ).filter { it.needsSync }

        for (record in items) {
            ensureUser(userId)

            val item = gson.fromJson(
                record.dataJson,
                PackingItem::class.java
            )

            val serverListId = mapping(
                userId,
                "packing_list",
                item.packingListId
            ) ?: throw IllegalStateException(
                "Packing item ${item.name} is waiting for its list."
            )

            var serverId = mapping(
                userId,
                "packing_item",
                record.recordId
            )

            if (serverId == null) {
                val serverList = remote.firstOrNull {
                    string(
                        it.asJsonObject,
                        "packingListId"
                    ) == serverListId
                }?.asJsonObject

                val serverItems =
                    serverList?.getAsJsonArray("items")

                val existing = serverItems?.firstOrNull {
                    string(
                        it.asJsonObject,
                        "packingItemId"
                    ) == item.packingItemId
                }

                if (existing != null) {
                    serverId = item.packingItemId
                    saveMapping(
                        userId,
                        "packing_item",
                        record.recordId,
                        serverId
                    )
                }
            }

            val body = JsonObject().apply {
                addProperty("name", item.name)
                addProperty(
                    "isPacked",
                    item.isPacked
                )
            }

            if (serverId == null) {
                createMapped(
                    userId,
                    record,
                    "packing_item",
                    "api/packing/$serverListId/items",
                    "item",
                    "packingItemId",
                    body,
                    auth
                )
            } else {
                updateMapped(
                    userId,
                    record,
                    "api/packing/items/$serverId",
                    body,
                    auth
                )
            }
        }

        deleteRecords(
            userId,
            auth,
            "packing_item",
            "api/packing/items"
        )

        deleteRecords(
            userId,
            auth,
            "packing_list",
            "api/packing"
        )
    }

    private suspend fun syncItineraries(
        userId: String,
        auth: String
    ) {
        val pendingRecords = pending(userId)
            .filter {
                it.recordType.startsWith("itinerary_")
            }

        val holidayIds = pendingRecords
            .mapNotNull {
                it.recordId.substringBefore(":")
                    .toIntOrNull()
            }
            .distinct()

        val holidayMappings =
            HolidayIdMappingStore(appContext)

        for (localHolidayId in holidayIds) {
            ensureUser(userId)

            val prefix = "$localHolidayId:"

            val days = dao.getRecords(
                userId,
                "itinerary_day"
            ).filter {
                it.recordId.startsWith(prefix) &&
                        it.needsSync
            }

            val activities = dao.getRecords(
                userId,
                "itinerary_activity"
            ).filter {
                it.recordId.startsWith(prefix) &&
                        it.needsSync
            }

            if (days.isNotEmpty() || activities.isNotEmpty()) {
                val serverHolidayId =
                    holidayMappings.getServerId(
                        localHolidayId
                    ) ?: throw IllegalStateException(
                        "Itinerary is waiting for holiday $localHolidayId to synchronize."
                    )

                val itinerary = getOrCreateItinerary(
                    localHolidayId,
                    serverHolidayId,
                    auth
                )

                val itineraryId = string(
                    itinerary,
                    "itineraryId"
                ) ?: throw IllegalStateException(
                    "Azure itinerary ID is missing."
                )

                val remoteDays =
                    itinerary.getAsJsonArray("days")
                        ?: JsonArray()

                for (record in days) {
                    ensureUser(userId)

                    val day = gson.fromJson(
                        record.dataJson,
                        ItineraryDay::class.java
                    )

                    val existing = remoteDays.firstOrNull {
                        it.asJsonObject.get("dayNumber")
                            ?.asInt == day.dayNumber
                    }?.asJsonObject

                    val serverDayId: String

                    if (existing == null) {
                        val body = JsonObject().apply {
                            addProperty(
                                "dayNumber",
                                day.dayNumber
                            )
                            addProperty("date", day.date)
                        }

                        val response = api.post(
                            "api/itineraries/$itineraryId/days",
                            auth,
                            body
                        )

                        val created = successful(
                            response
                        ).getAsJsonObject("day")
                            ?: throw IllegalStateException(
                                "Azure itinerary day is missing."
                            )

                        serverDayId = string(
                            created,
                            "itineraryDayId"
                        ) ?: throw IllegalStateException(
                            "Azure itinerary day ID is missing."
                        )

                        remoteDays.add(created)
                    } else {
                        serverDayId = string(
                            existing,
                            "itineraryDayId"
                        ) ?: throw IllegalStateException(
                            "Azure itinerary day ID is invalid."
                        )

                        val remoteDate = string(
                            existing,
                            "date"
                        )?.take(10)

                        if (remoteDate != day.date) {
                            val body = JsonObject().apply {
                                addProperty(
                                    "dayNumber",
                                    day.dayNumber
                                )
                                addProperty(
                                    "date",
                                    day.date
                                )
                            }

                            successful(
                                api.patch(
                                    "api/itineraries/days/$serverDayId",
                                    auth,
                                    body
                                )
                            )
                        }
                    }

                    database.withTransaction {
                        saveMapping(
                            userId,
                            "itinerary_day",
                            record.recordId,
                            serverDayId
                        )

                        acknowledge(userId, record)
                    }

                    uploaded++
                }

                for (record in activities) {
                    ensureUser(userId)

                    val activity = gson.fromJson(
                        record.dataJson,
                        ItineraryActivity::class.java
                    )

                    val serverDay = remoteDays.firstOrNull {
                        it.asJsonObject.get("dayNumber")
                            ?.asInt == activity.dayNumber
                    }?.asJsonObject ?: throw IllegalStateException(
                        "Activity ${activity.title} is waiting for its itinerary day."
                    )

                    val serverDayId = string(
                        serverDay,
                        "itineraryDayId"
                    ) ?: throw IllegalStateException(
                        "Server itinerary day ID is missing."
                    )

                    val body = JsonObject().apply {
                        addProperty(
                            "title",
                            activity.title
                        )
                        addProperty(
                            "startTime",
                            activity.startTime
                        )

                        if (activity.endTime != null) {
                            addProperty(
                                "endTime",
                                activity.endTime
                            )
                        }

                        if (activity.location != null) {
                            addProperty(
                                "location",
                                activity.location
                            )
                        }

                        if (activity.notes != null) {
                            addProperty(
                                "notes",
                                activity.notes
                            )
                        }
                    }

                    val serverId = mapping(
                        userId,
                        "itinerary_activity",
                        record.recordId
                    )

                    if (serverId == null) {
                        createMapped(
                            userId,
                            record,
                            "itinerary_activity",
                            "api/itineraries/days/$serverDayId/activities",
                            "activity",
                            "activityId",
                            body,
                            auth
                        )
                    } else {
                        updateMapped(
                            userId,
                            record,
                            "api/itineraries/activities/$serverId",
                            body,
                            auth
                        )
                    }
                }
            }
        }

        deleteRecords(
            userId,
            auth,
            "itinerary_activity",
            "api/itineraries/activities"
        )

        deleteRecords(
            userId,
            auth,
            "itinerary_day",
            "api/itineraries/days"
        )
    }

    private suspend fun getOrCreateItinerary(
        localHolidayId: Int,
        serverHolidayId: String,
        auth: String
    ): JsonObject {
        val path =
            "api/itineraries/holiday/$serverHolidayId"

        val existing = api.get(path, auth)

        if (existing.isSuccessful) {
            return successful(existing)
                .getAsJsonObject("itinerary")
                ?: throw IllegalStateException(
                    "Azure itinerary response is missing."
                )
        }

        check(existing.code() == 404) {
            "Unable to retrieve itinerary. HTTP ${existing.code()}"
        }

        val body = JsonObject().apply {
            addProperty(
                "holidayId",
                serverHolidayId
            )
            addProperty(
                "name",
                "Holiday $localHolidayId Itinerary"
            )
        }

        val created = api.post(
            "api/itineraries",
            auth,
            body
        )

        check(
            created.isSuccessful ||
                    created.code() == 409
        ) {
            "Unable to create itinerary. HTTP ${created.code()}"
        }

        return get(path, auth)
            .getAsJsonObject("itinerary")
            ?: throw IllegalStateException(
                "New itinerary could not be retrieved."
            )
    }

    private suspend fun createMapped(
        userId: String,
        record: OfflineTravelRecordEntity,
        type: String,
        path: String,
        responseField: String,
        idField: String,
        body: JsonObject,
        auth: String
    ) {
        val attemptType = "sync_attempt_$type"

        val previous = dao.getRecordById(
            userId,
            attemptType,
            record.recordId
        )

        check(previous == null) {
            "$type has an unresolved creation attempt. Automatic retry is blocked."
        }

        dao.saveRecord(
            OfflineTravelRecordEntity(
                userId = userId,
                recordType = attemptType,
                recordId = record.recordId,
                dataJson = body.toString(),
                updatedAtMillis =
                    System.currentTimeMillis(),
                needsSync = false
            )
        )

        val response = api.post(
            path,
            auth,
            body
        )

        if (!response.isSuccessful) {
            if (
                response.code() in 400..499 &&
                response.code() != 408 &&
                response.code() != 409 &&
                response.code() != 429
            ) {
                dao.deleteRecord(
                    userId,
                    attemptType,
                    record.recordId
                )
            }

            throw IllegalStateException(
                "$type creation failed. HTTP ${response.code()}"
            )
        }

        val serverId = string(
            successful(response)
                .getAsJsonObject(responseField),
            idField
        ) ?: throw IllegalStateException(
            "Azure did not return the $type ID."
        )

        database.withTransaction {
            saveMapping(
                userId,
                type,
                record.recordId,
                serverId
            )

            acknowledge(userId, record)

            dao.deleteRecord(
                userId,
                attemptType,
                record.recordId
            )
        }

        uploaded++
    }

    private suspend fun updateMapped(
        userId: String,
        record: OfflineTravelRecordEntity,
        path: String,
        body: JsonObject,
        auth: String
    ) {
        successful(
            api.patch(path, auth, body)
        )

        database.withTransaction {
            acknowledge(userId, record)
        }

        uploaded++
    }

    private suspend fun deleteRecords(
        userId: String,
        auth: String,
        type: String,
        pathPrefix: String
    ) {
        val deletionType = "${type}_deletion"

        val records = dao.getRecords(
            userId,
            deletionType
        ).filter { it.needsSync }

        for (record in records) {
            ensureUser(userId)

            val serverId = mapping(
                userId,
                type,
                record.recordId
            )

            if (serverId != null) {
                val response = api.delete(
                    "$pathPrefix/$serverId",
                    auth
                )

                check(
                    response.isSuccessful ||
                            response.code() == 404
                ) {
                    "$type deletion failed. HTTP ${response.code()}"
                }
            } else {
                val attempt = dao.getRecordById(
                    userId,
                    "sync_attempt_$type",
                    record.recordId
                )

                check(attempt == null) {
                    "$type deletion requires reconciliation."
                }
            }

            database.withTransaction {
                val current = dao.getRecordById(
                    userId,
                    deletionType,
                    record.recordId
                )

                if (
                    current != null &&
                    current.dataJson == record.dataJson &&
                    current.updatedAtMillis ==
                    record.updatedAtMillis
                ) {
                    dao.deleteRecord(
                        userId,
                        deletionType,
                        record.recordId
                    )

                    dao.deleteRecord(
                        userId,
                        "sync_mapping_$type",
                        record.recordId
                    )
                }
            }

            deleted++
        }
    }

    private suspend fun mapping(
        userId: String,
        type: String,
        localId: String
    ): String? {
        val record = dao.getRecordById(
            userId,
            "sync_mapping_$type",
            localId
        ) ?: return null

        val data = gson.fromJson(
            record.dataJson,
            JsonObject::class.java
        )

        return string(data, "serverId")
    }

    private suspend fun saveMapping(
        userId: String,
        type: String,
        localId: String,
        serverId: String
    ) {
        val validatedId =
            UUID.fromString(serverId).toString()

        val existing = mapping(
            userId,
            type,
            localId
        )

        check(
            existing == null ||
                    existing == validatedId
        ) {
            "Conflicting Azure ID mapping."
        }

        val data = JsonObject().apply {
            addProperty(
                "serverId",
                validatedId
            )
        }

        dao.saveRecord(
            OfflineTravelRecordEntity(
                userId = userId,
                recordType = "sync_mapping_$type",
                recordId = localId,
                dataJson = data.toString(),
                updatedAtMillis =
                    System.currentTimeMillis(),
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
            current.updatedAtMillis ==
            snapshot.updatedAtMillis
        ) {
            dao.saveRecord(
                current.copy(needsSync = false)
            )
        }
    }

    private suspend fun pending(
        userId: String
    ): List<OfflineTravelRecordEntity> {
        val types = setOf(
            "packing_list",
            "packing_item",
            "packing_list_deletion",
            "packing_item_deletion",
            "itinerary_day",
            "itinerary_activity",
            "itinerary_day_deletion",
            "itinerary_activity_deletion"
        )

        return dao.getPendingRecords(userId)
            .filter {
                it.recordType in types
            }
    }

    private suspend fun get(
        path: String,
        auth: String
    ): JsonObject {
        return successful(
            api.get(path, auth)
        )
    }

    private fun successful(
        response: Response<JsonObject>
    ): JsonObject {
        check(response.isSuccessful) {
            "Azure request failed. HTTP ${response.code()}"
        }

        return response.body()
            ?: throw IllegalStateException(
                "Azure response body is missing."
            )
    }

    private fun string(
        data: JsonObject?,
        name: String
    ): String? {
        val value = data?.get(name)
            ?: return null

        if (value.isJsonNull) return null

        return value.asString
    }

    private fun ensureUser(
        userId: String
    ) {
        check(
            sessions.getSession()?.userId == userId
        ) {
            "Authenticated user changed during synchronization."
        }
    }

    companion object {
        private val mutex = Mutex()
    }
}
