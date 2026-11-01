
package com.example.roamly.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.OfflineHolidayEntity
import com.example.roamly.data.local.OfflineTravelRecordEntity
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.sync.RoamlySyncScheduler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RoomHolidayRepository(
    context: Context
) {

    private val appContext = context.applicationContext

    private val userId =
        SessionManager(appContext).getUserId()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Sign in to access holidays."
            )

    private val preferences =
        appContext.getSharedPreferences(
            "roamly_holidays_$userId",
            Context.MODE_PRIVATE
        )

    private val database =
        RoamlyOfflineDatabase.getInstance(
            appContext
        )

    private val holidayDao =
        database.holidayDao()

    private val recordDao =
        database.travelRecordDao()

    private val gson = Gson()

    suspend fun getHolidays(): List<Holiday> {
        ensureMigrated()

        return holidayDao.getHolidays(userId)
            .map { decode(it) }
    }

    fun observeHolidays(): Flow<List<Holiday>> =
        flow {
            ensureMigrated()

            emitAll(
                holidayDao.observeHolidays(userId)
                    .map { entities ->
                        entities.map { decode(it) }
                    }
            )
        }

    suspend fun getHolidayById(
        holidayId: Int
    ): Holiday? {
        ensureMigrated()

        return holidayDao.getHolidayById(
            userId,
            holidayId
        )?.let { decode(it) }
    }

    suspend fun createHoliday(
        holiday: Holiday
    ): Holiday {
        ensureMigrated()

        val createdHoliday = database.withTransaction {
            val highestHolidayId =
                holidayDao.getHolidays(userId)
                    .maxOfOrNull {
                        it.holidayId
                    } ?: 0

            val highestDeletedId =
                recordDao.getRecords(
                    userId,
                    "holiday_deletion"
                ).mapNotNull {
                    it.recordId.toIntOrNull()
                }.maxOrNull() ?: 0

            val nextId =
                maxOf(
                    highestHolidayId,
                    highestDeletedId
                ) + 1

            val created =
                holiday.copy(
                    holidayId = nextId
                )

            holidayDao.saveHoliday(
                createEntity(created)
            )

            created
        }

        RoamlySyncScheduler.requestNow(appContext)

        return createdHoliday
    }

    suspend fun updateHoliday(
        holiday: Holiday
    ) {
        ensureMigrated()

        database.withTransaction {
            val existing =
                holidayDao.getHolidayById(
                    userId,
                    holiday.holidayId
                ) ?: throw IllegalStateException(
                    "Holiday no longer exists."
                )

            holidayDao.saveHoliday(
                createEntity(
                    holiday,
                    existing
                )
            )
        }

        RoamlySyncScheduler.requestNow(appContext)
    }

    suspend fun deleteHoliday(
        holidayId: Int
    ): Boolean {
        ensureMigrated()

        val deleted = database.withTransaction {
            val existing =
                holidayDao.getHolidayById(
                    userId,
                    holidayId
                )

            if (existing == null) {
                false
            } else {
                recordDao.saveRecord(
                    deletionRecord(holidayId)
                )

                holidayDao.deleteHoliday(
                    userId,
                    holidayId
                )

                true
            }
        }

        if (deleted) {
            RoamlySyncScheduler.requestNow(appContext)
        }

        return deleted
    }

    private suspend fun ensureMigrated() {
        withContext(Dispatchers.IO) {
            migrationMutex.withLock {
                if (
                    preferences.getBoolean(
                        MIGRATION_KEY,
                        false
                    )
                ) {
                    return@withLock
                }

                val json =
                    preferences.getString(
                        HOLIDAYS_KEY,
                        null
                    )

                val legacyHolidays =
                    if (json == null) {
                        null
                    } else {
                        val type =
                            object :
                                TypeToken<List<Holiday>>() {}
                                .type

                        gson.fromJson<List<Holiday>>(
                            json,
                            type
                        ) ?: throw IllegalStateException(
                            "Saved holidays could not be read."
                        )
                    }

                database.withTransaction {
                    if (legacyHolidays != null) {
                        val existing =
                            holidayDao.getHolidays(
                                userId
                            ).associateBy {
                                it.holidayId
                            }

                        val legacyById =
                            legacyHolidays.associateBy {
                                it.holidayId
                            }

                        for (holiday in legacyById.values) {
                            val previous =
                                existing[holiday.holidayId]

                            val currentJson =
                                gson.toJson(holiday)

                            if (
                                previous == null ||
                                previous.holidayJson !=
                                currentJson
                            ) {
                                holidayDao.saveHoliday(
                                    createEntity(
                                        holiday,
                                        previous
                                    )
                                )
                            }

                            recordDao.deleteRecord(
                                userId,
                                "holiday_deletion",
                                holiday.holidayId.toString()
                            )
                        }

                        for (previous in existing.values) {
                            if (
                                previous.holidayId !in
                                legacyById
                            ) {
                                recordDao.saveRecord(
                                    deletionRecord(
                                        previous.holidayId
                                    )
                                )

                                holidayDao.deleteHoliday(
                                    userId,
                                    previous.holidayId
                                )
                            }
                        }
                    }
                }

                check(
                    preferences.edit()
                        .putBoolean(
                            MIGRATION_KEY,
                            true
                        )
                        .commit()
                ) {
                    "Holiday migration could not be recorded."
                }
            }
        }
    }

    private fun createEntity(
        holiday: Holiday,
        existing: OfflineHolidayEntity? = null
    ): OfflineHolidayEntity {
        return OfflineHolidayEntity(
            userId = userId,
            holidayId = holiday.holidayId,
            name = holiday.name,
            location = holiday.location,
            startDate = holiday.startDate,
            endDate = holiday.endDate,
            holidayJson = gson.toJson(holiday),
            updatedAtMillis = System.currentTimeMillis(),
            needsSync = true
        )
    }

    private fun deletionRecord(
        holidayId: Int
    ): OfflineTravelRecordEntity {
        return OfflineTravelRecordEntity(
            userId = userId,
            recordType = "holiday_deletion",
            recordId = holidayId.toString(),
            dataJson = gson.toJson(
                mapOf(
                    "holidayId" to holidayId
                )
            ),
            updatedAtMillis =
                System.currentTimeMillis(),
            needsSync = true
        )
    }

    private fun decode(
        entity: OfflineHolidayEntity
    ): Holiday {
        return gson.fromJson(
            entity.holidayJson,
            Holiday::class.java
        ) ?: throw IllegalStateException(
            "Stored holiday could not be read."
        )
    }

    companion object {
        private const val HOLIDAYS_KEY =
            "holidays"

        private const val MIGRATION_KEY =
            "room_holidays_migrated_v1"

        private val migrationMutex =
            Mutex()
    }
}
