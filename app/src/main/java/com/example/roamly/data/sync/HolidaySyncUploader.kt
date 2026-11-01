
package com.example.roamly.data.sync

import android.content.Context
import androidx.room.withTransaction
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineHolidayEntity
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class HolidayUploadResult(
    val uploaded: Int,
    val deleted: Int,
    val remaining: Int,
    val success: Boolean,
    val message: String
)

class HolidaySyncUploader(
    context: Context
) {
    private val appContext =
        context.applicationContext

    private val sessions =
        SessionManager(appContext)

    private val database =
        RoamlyOfflineDatabase.getInstance(
            appContext
        )

    private val holidayDao =
        database.holidayDao()

    private val recordDao =
        database.travelRecordDao()

    private val gson = Gson()

    private val api =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SyncApiService::class.java)

    suspend fun uploadPending(): HolidayUploadResult =
        withContext(Dispatchers.IO) {
            uploadMutex.withLock {
                val session =
                    sessions.getSession()
                        ?: return@withLock HolidayUploadResult(
                            0, 0, 0, false,
                            "Sign in required."
                        )

                val userId = session.userId

                val mappings =
                    HolidayIdMappingStore(
                        appContext
                    )

                val authorization =
                    "Bearer ${session.accessToken}"

                var uploaded = 0
                var deleted = 0

                try {
                    val pending =
                        holidayDao.getHolidays(
                            userId
                        ).filter {
                            it.needsSync
                        }

                    val deletions =
                        recordDao.getRecords(
                            userId,
                            DELETION_TYPE
                        ).filter {
                            it.needsSync
                        }

                    if (
                        pending.isEmpty() &&
                        deletions.isEmpty()
                    ) {
                        return@withLock HolidayUploadResult(
                            0, 0, 0, true,
                            "No pending holiday changes."
                        )
                    }

                    val health =
                        api.checkDatabase()

                    check(health.isSuccessful) {
                        "Azure database unavailable. HTTP ${health.code()}"
                    }

                    for (entity in pending) {
                        check(
                            sessions.getSession()
                                ?.userId == userId
                        ) {
                            "The signed-in user changed."
                        }

                        val serverId =
                            mappings.getServerId(
                                entity.holidayId
                            )

                        if (serverId == null) {
                            createOnServer(
                                entity,
                                userId,
                                authorization,
                                mappings
                            )
                        } else {
                            updateOnServer(
                                entity,
                                userId,
                                serverId,
                                authorization
                            )
                        }

                        uploaded++
                    }

                    for (record in deletions) {
                        check(
                            sessions.getSession()
                                ?.userId == userId
                        ) {
                            "The signed-in user changed."
                        }

                        deleteOnServer(
                            record,
                            userId,
                            authorization,
                            mappings
                        )

                        deleted++
                    }

                    val remaining =
                        countRemaining(userId)

                    HolidayUploadResult(
                        uploaded = uploaded,
                        deleted = deleted,
                        remaining = remaining,
                        success = remaining == 0,
                        message = if (remaining == 0) {
                            "All holiday changes uploaded."
                        } else {
                            "$remaining holiday changes remain pending."
                        }
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    HolidayUploadResult(
                        uploaded = uploaded,
                        deleted = deleted,
                        remaining = countRemaining(userId),
                        success = false,
                        message = error.message
                            ?: "Synchronization failed."
                    )
                }
            }
        }

    private suspend fun createOnServer(
        entity: OfflineHolidayEntity,
        userId: String,
        authorization: String,
        mappings: HolidayIdMappingStore
    ) {
        val holiday =
            decodeHoliday(entity)

        val body =
            holidayBody(holiday)

        val attemptId =
            entity.holidayId.toString()

        val previousAttempt =
            recordDao.getRecordById(
                userId,
                CREATE_ATTEMPT_TYPE,
                attemptId
            )

        check(previousAttempt == null) {
            "Holiday ${entity.holidayId} has an unresolved create attempt. Automatic retry is blocked to prevent duplicates."
        }

        recordDao.saveRecord(
            OfflineTravelRecordEntity(
                userId = userId,
                recordType = CREATE_ATTEMPT_TYPE,
                recordId = attemptId,
                dataJson = body.toString(),
                updatedAtMillis =
                    System.currentTimeMillis(),
                needsSync = false
            )
        )

        val response = api.createHoliday(
            authorization,
            body
        )

        if (!response.isSuccessful) {
            if (response.code() in 400..499 && response.code() !in listOf(408, 409, 429)) {
                recordDao.deleteRecord(
                    userId,
                    CREATE_ATTEMPT_TYPE,
                    attemptId
                )
            }

            throw IllegalStateException(
                "Holiday creation failed. HTTP ${response.code()}"
            )
        }

        val serverId =
            response.body()
                ?.getAsJsonObject("holiday")
                ?.get("holidayId")
                ?.asString
                ?: throw IllegalStateException(
                    "Azure did not return a holiday ID."
                )

        database.withTransaction {
            mappings.saveMapping(
                entity.holidayId,
                serverId
            )

            acknowledgeHoliday(
                entity,
                userId
            )

            recordDao.deleteRecord(
                userId,
                CREATE_ATTEMPT_TYPE,
                attemptId
            )
        }
    }

    private suspend fun updateOnServer(
        entity: OfflineHolidayEntity,
        userId: String,
        serverId: String,
        authorization: String
    ) {
        val body = holidayBody(
            decodeHoliday(entity)
        )

        val response = api.updateHoliday(
            authorization,
            serverId,
            body
        )

        check(response.isSuccessful) {
            "Holiday ${entity.holidayId} update failed. HTTP ${response.code()}"
        }

        database.withTransaction {
            acknowledgeHoliday(
                entity,
                userId
            )
        }
    }

    private suspend fun deleteOnServer(
        record: OfflineTravelRecordEntity,
        userId: String,
        authorization: String,
        mappings: HolidayIdMappingStore
    ) {
        val localId =
            record.recordId.toIntOrNull()
                ?: throw IllegalStateException(
                    "Invalid local holiday deletion ID."
                )

        val serverId =
            mappings.getServerId(localId)

        if (serverId == null) {
            val attempt =
                recordDao.getRecordById(
                    userId,
                    CREATE_ATTEMPT_TYPE,
                    record.recordId
                )

            check(attempt == null) {
                "Holiday $localId may already exist on Azure. Its deletion requires reconciliation."
            }
        } else {
            val response =
                api.deleteHoliday(
                    authorization,
                    serverId
                )

            check(
                response.isSuccessful ||
                        response.code() == 404
            ) {
                "Holiday $localId deletion failed. HTTP ${response.code()}"
            }
        }

        database.withTransaction {
            val current =
                recordDao.getRecordById(
                    userId,
                    DELETION_TYPE,
                    record.recordId
                )

            if (
                current != null &&
                current.updatedAtMillis ==
                record.updatedAtMillis &&
                current.dataJson == record.dataJson
            ) {
                recordDao.deleteRecord(
                    userId,
                    DELETION_TYPE,
                    record.recordId
                )

                recordDao.deleteRecord(
                    userId,
                    MAPPING_TYPE,
                    record.recordId
                )
            }
        }
    }

    private suspend fun acknowledgeHoliday(
        snapshot: OfflineHolidayEntity,
        userId: String
    ) {
        val current =
            holidayDao.getHolidayById(
                userId,
                snapshot.holidayId
            ) ?: return

        if (
            current.needsSync &&
            current.holidayJson ==
            snapshot.holidayJson &&
            current.updatedAtMillis ==
            snapshot.updatedAtMillis
        ) {
            holidayDao.saveHoliday(
                current.copy(
                    needsSync = false
                )
            )
        }
    }

    private fun decodeHoliday(
        entity: OfflineHolidayEntity
    ): Holiday {
        return gson.fromJson(
            entity.holidayJson,
            Holiday::class.java
        ) ?: throw IllegalStateException(
            "Saved holiday data is invalid."
        )
    }

    private fun holidayBody(
        holiday: Holiday
    ): JsonObject {
        fun requiredText(
            value: String,
            field: String
        ): String {
            val cleaned = value.trim()

            require(
                cleaned.isNotEmpty() &&
                        cleaned.length <= 200
            ) {
                "$field is required for Azure synchronization."
            }

            return cleaned
        }

        require(
            DATE_PATTERN.matches(holiday.startDate) &&
                    DATE_PATTERN.matches(holiday.endDate)
        ) {
            "Holiday dates must use YYYY-MM-DD."
        }

        require(
            holiday.endDate >= holiday.startDate
        ) {
            "Holiday end date is before the start date."
        }

        return JsonObject().apply {
            addProperty(
                "title",
                requiredText(
                    holiday.name,
                    "Holiday name"
                )
            )

            addProperty(
                "tripType",
                requiredText(
                    holiday.tripType,
                    "Trip type"
                )
            )

            addProperty(
                "country",
                requiredText(
                    holiday.country,
                    "Country"
                )
            )

            addProperty(
                "city",
                requiredText(
                    holiday.city,
                    "City"
                )
            )

            addProperty(
                "startDate",
                holiday.startDate
            )

            addProperty(
                "endDate",
                holiday.endDate
            )

            val imageUrl =
                holiday.coverImageUri
                    ?.takeIf {
                        it.startsWith(
                            "https://",
                            ignoreCase = true
                        )
                    }

            if (imageUrl != null) {
                addProperty(
                    "imageUrl",
                    imageUrl
                )
            }
        }
    }

    private suspend fun countRemaining(
        userId: String
    ): Int {
        val holidays =
            holidayDao.getHolidays(
                userId
            ).count {
                it.needsSync
            }

        val deletions =
            recordDao.getRecords(
                userId,
                DELETION_TYPE
            ).count {
                it.needsSync
            }

        return holidays + deletions
    }

    companion object {
        private const val DELETION_TYPE =
            "holiday_deletion"

        private const val MAPPING_TYPE =
            "holiday_server_mapping"

        private const val CREATE_ATTEMPT_TYPE =
            "holiday_create_attempt"

        private val DATE_PATTERN =
            Regex("\\d{4}-\\d{2}-\\d{2}")

        private val uploadMutex = Mutex()
    }
}
