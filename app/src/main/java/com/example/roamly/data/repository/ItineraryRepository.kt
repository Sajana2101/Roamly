
package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.RoomTravelStore
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ItineraryRepository(
    context: Context
) {
    private val userId =
        SessionManager(context).getUserId()
            ?: throw IllegalStateException(
                "Sign in to access itineraries."
            )

    private val store = RoomTravelStore(context)

    private val preferenceName =
        "roamly_itinerary_$userId"

    fun getItineraryDays(
        holidayId: Int
    ): MutableList<ItineraryDay> =
        runBlocking(Dispatchers.IO) {
            store.read(
                "itinerary_day",
                preferenceName,
                "itinerary_days_$holidayId",
                ItineraryDay::class.java,
                { "$holidayId:${it.dayNumber}" },
                "$holidayId:"
            )
        }

    fun saveItineraryDays(
        holidayId: Int,
        itineraryDays: List<ItineraryDay>
    ) {
        runBlocking(Dispatchers.IO) {
            store.replace(
                "itinerary_day",
                preferenceName,
                "itinerary_days_$holidayId",
                ItineraryDay::class.java,
                itineraryDays,
                { "$holidayId:${it.dayNumber}" },
                "$holidayId:"
            )
        }
    }

    fun getItineraryActivities(
        holidayId: Int
    ): MutableList<ItineraryActivity> =
        runBlocking(Dispatchers.IO) {
            store.read(
                "itinerary_activity",
                preferenceName,
                "itinerary_activities_$holidayId",
                ItineraryActivity::class.java,
                { "$holidayId:${it.activityId}" },
                "$holidayId:"
            )
        }

    fun saveItineraryActivities(
        holidayId: Int,
        itineraryActivities: List<ItineraryActivity>
    ) {
        runBlocking(Dispatchers.IO) {
            store.replace(
                "itinerary_activity",
                preferenceName,
                "itinerary_activities_$holidayId",
                ItineraryActivity::class.java,
                itineraryActivities,
                { "$holidayId:${it.activityId}" },
                "$holidayId:"
            )
        }
    }

    fun deleteItinerary(
        holidayId: Int
    ) {
        saveItineraryDays(
            holidayId,
            emptyList()
        )

        saveItineraryActivities(
            holidayId,
            emptyList()
        )
    }
}
