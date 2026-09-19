package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.model.Holiday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HolidayRepository(
    context: Context
) {

    private val sharedPreferences =
        context.getSharedPreferences(
            "roamly_holidays",
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    fun getHolidays(): MutableList<Holiday> {
        val json =
            sharedPreferences.getString(
                HOLIDAYS_KEY,
                null
            ) ?: return mutableListOf()

        return try {
            val type =
                object :
                    TypeToken<MutableList<Holiday>>() {}
                    .type

            gson.fromJson(
                json,
                type
            )
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun getHolidayById(
        holidayId: Int
    ): Holiday? {
        return getHolidays()
            .firstOrNull {
                it.holidayId == holidayId
            }
    }

    fun addHoliday(
        holiday: Holiday
    ) {
        val holidays =
            getHolidays()

        holidays.add(holiday)

        saveHolidays(holidays)
    }

    fun updateHoliday(
        holiday: Holiday
    ) {
        val holidays =
            getHolidays()

        val index =
            holidays.indexOfFirst {
                it.holidayId ==
                        holiday.holidayId
            }

        if (index >= 0) {
            holidays[index] = holiday

            saveHolidays(holidays)
        }
    }

    fun deleteHoliday(
        holidayId: Int
    ) {
        val holidays =
            getHolidays()

        holidays.removeAll {
            it.holidayId == holidayId
        }

        saveHolidays(holidays)
    }

    fun getNextHolidayId(): Int {
        val holidays =
            getHolidays()

        val highestId =
            holidays.maxOfOrNull {
                it.holidayId
            } ?: 0

        return highestId + 1
    }

    private fun saveHolidays(
        holidays: List<Holiday>
    ) {
        val json =
            gson.toJson(holidays)

        sharedPreferences
            .edit()
            .putString(
                HOLIDAYS_KEY,
                json
            )
            .apply()
    }

    companion object {
        private const val HOLIDAYS_KEY =
            "holidays"
    }
}