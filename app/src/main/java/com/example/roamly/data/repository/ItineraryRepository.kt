
package com.example.roamly.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.RoomTravelStore
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import com.example.roamly.data.sync.ItineraryUploadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class ItineraryRepository(
    context: Context
) {

    private val appContext = context.applicationContext

    private val userId =
        SessionManager(appContext).getUserId()
            ?: throw IllegalStateException(
                "Sign in to access itineraries."
            )

    private val store = RoomTravelStore(appContext)

    private val preferenceName =
        "roamly_itinerary_$userId"

    init {
        requestUpload()
    }

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

        requestUpload()
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

        requestUpload()
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

    private fun requestUpload() {
        val request =
            OneTimeWorkRequestBuilder<ItineraryUploadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            NetworkType.CONNECTED
                        )
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                "roamly_itinerary_upload",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
    }
}
