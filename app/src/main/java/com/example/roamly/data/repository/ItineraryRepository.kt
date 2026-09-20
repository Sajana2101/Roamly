package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ItineraryRepository(
    context: Context
) {

    private val userId =
        SessionManager(
            context
        ).getUserId()
            ?: "guest"

    private val sharedPreferences =
        context.getSharedPreferences(
            "roamly_itinerary_${userId}",
            Context.MODE_PRIVATE
        )

    private val gson =
        Gson()

    fun getItineraryDays(
        holidayId: Int
    ): MutableList<ItineraryDay> {

        val json =
            sharedPreferences.getString(
                itineraryDaysKey(
                    holidayId
                ),
                null
            ) ?: return mutableListOf()

        return try {

            val type =
                object :
                    TypeToken<MutableList<ItineraryDay>>() {}
                    .type

            gson.fromJson<MutableList<ItineraryDay>>(
                json,
                type
            ) ?: mutableListOf()

        } catch (_: Exception) {

            mutableListOf()
        }
    }

    fun saveItineraryDays(
        holidayId: Int,
        itineraryDays:
        List<ItineraryDay>
    ) {

        sharedPreferences
            .edit()
            .putString(
                itineraryDaysKey(
                    holidayId
                ),
                gson.toJson(
                    itineraryDays
                )
            )
            .apply()
    }

    fun getItineraryActivities(
        holidayId: Int
    ): MutableList<ItineraryActivity> {

        val json =
            sharedPreferences.getString(
                itineraryActivitiesKey(
                    holidayId
                ),
                null
            ) ?: return mutableListOf()

        return try {

            val type =
                object :
                    TypeToken<MutableList<ItineraryActivity>>() {}
                    .type

            gson.fromJson<MutableList<ItineraryActivity>>(
                json,
                type
            ) ?: mutableListOf()

        } catch (_: Exception) {

            mutableListOf()
        }
    }

    fun saveItineraryActivities(
        holidayId: Int,
        itineraryActivities:
        List<ItineraryActivity>
    ) {

        sharedPreferences
            .edit()
            .putString(
                itineraryActivitiesKey(
                    holidayId
                ),
                gson.toJson(
                    itineraryActivities
                )
            )
            .apply()
    }

    fun deleteItinerary(
        holidayId: Int
    ) {

        sharedPreferences
            .edit()
            .remove(
                itineraryDaysKey(
                    holidayId
                )
            )
            .remove(
                itineraryActivitiesKey(
                    holidayId
                )
            )
            .apply()
    }

    private fun itineraryDaysKey(
        holidayId: Int
    ): String {

        return "itinerary_days_$holidayId"
    }

    private fun itineraryActivitiesKey(
        holidayId: Int
    ): String {

        return "itinerary_activities_$holidayId"
    }
}
