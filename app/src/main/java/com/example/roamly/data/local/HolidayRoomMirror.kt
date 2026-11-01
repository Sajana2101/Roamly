
package com.example.roamly.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.roamly.data.model.Holiday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object HolidayRoomMirror {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val mutex = Mutex()

    private val gson = Gson()

    fun schedule(
        context: Context,
        userId: String
    ) {
        if (userId == "guest") {
            return
        }

        val applicationContext =
            context.applicationContext

        scope.launch {
            mutex.withLock {
                try {
                    mirrorHolidays(
                        applicationContext,
                        userId
                    )
                } catch (error: Exception) {
                    Log.e(
                        "HolidayRoomMirror",
                        "Unable to mirror holidays",
                        error
                    )
                }
            }
        }
    }

    private suspend fun mirrorHolidays(
        context: Context,
        userId: String
    ) {
        val preferences =
            context.getSharedPreferences(
                "roamly_holidays_$userId",
                Context.MODE_PRIVATE
            )

        if (preferences.getBoolean("room_holidays_migrated_v1", false)) return

        val json = preferences.getString(
            "holidays",
            null
        ) ?: return

        val holidays = try {
            val type =
                object : TypeToken<List<Holiday>>() {}
                    .type

            gson.fromJson<List<Holiday>>(
                json,
                type
            ) ?: return
        } catch (error: Exception) {
            Log.e(
                "HolidayRoomMirror",
                "Unable to read saved holidays",
                error
            )

            return
        }

        val uniqueHolidays =
            holidays.distinctBy {
                it.holidayId
            }

        val database =
            RoamlyOfflineDatabase.getInstance(context)

        database.withTransaction {
            val holidayDao = database.holidayDao()
            val recordDao = database.travelRecordDao()

            val existingHolidays =
                holidayDao.getHolidays(userId)
                    .associateBy {
                        it.holidayId
                    }

            val currentIds =
                uniqueHolidays.map {
                    it.holidayId
                }.toSet()

            for (holiday in uniqueHolidays) {
                val holidayJson =
                    gson.toJson(holiday)

                val existing =
                    existingHolidays[holiday.holidayId]

                if (
                    existing == null ||
                    existing.holidayJson != holidayJson
                ) {
                    holidayDao.saveHoliday(
                        OfflineHolidayEntity(
                            userId = userId,
                            holidayId = holiday.holidayId,
                            name = holiday.name,
                            location = holiday.location,
                            startDate = holiday.startDate,
                            endDate = holiday.endDate,
                            holidayJson = holidayJson,
                            updatedAtMillis =
                                System.currentTimeMillis(),
                            needsSync = true
                        )
                    )
                }

                recordDao.deleteRecord(
                    userId,
                    "holiday_deletion",
                    holiday.holidayId.toString()
                )
            }

            for (existing in existingHolidays.values) {
                if (existing.holidayId !in currentIds) {
                    recordDao.saveRecord(
                        OfflineTravelRecordEntity(
                            userId = userId,
                            recordType = "holiday_deletion",
                            recordId =
                                existing.holidayId.toString(),
                            dataJson = gson.toJson(
                                mapOf(
                                    "holidayId" to
                                            existing.holidayId
                                )
                            ),
                            updatedAtMillis =
                                System.currentTimeMillis(),
                            needsSync = true
                        )
                    )

                    holidayDao.deleteHoliday(
                        userId,
                        existing.holidayId
                    )
                }
            }
        }
    }
}
