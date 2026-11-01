
package com.example.roamly.data.sync

import android.content.Context
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.local.RoamlyOfflineDatabase
import com.example.roamly.data.network.ApiConfig
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class SyncReadiness(
    val ready: Boolean,
    val pendingHolidayChanges: Int,
    val pendingTravelChanges: Int,
    val remoteHolidayCount: Int?,
    val remotePackingListCount: Int?,
    val remoteItineraryCount: Int?,
    val remoteChecklistCount: Int?,
    val message: String
)

class SyncReadinessChecker(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    private val database =
        RoamlyOfflineDatabase.getInstance(
            applicationContext
        )

    private val sessionManager =
        SessionManager(applicationContext)

    private val api =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SyncApiService::class.java)

    suspend fun check(): SyncReadiness =
        withContext(Dispatchers.IO) {
            val session =
                sessionManager.getSession()

            if (session == null) {
                return@withContext SyncReadiness(
                    ready = false,
                    pendingHolidayChanges = 0,
                    pendingTravelChanges = 0,
                    remoteHolidayCount = null,
                    remotePackingListCount = null,
                    remoteItineraryCount = null,
                    remoteChecklistCount = null,
                    message = "Sign in required."
                )
            }

            val userId = session.userId

            val pendingHolidays =
                database.holidayDao()
                    .getHolidays(userId)
                    .count {
                        it.needsSync
                    }

            val pendingTravel =
                database.travelRecordDao()
                    .getPendingRecords(userId)
                    .size

            try {
                val health = api.checkDatabase()

                if (!health.isSuccessful) {
                    return@withContext unavailable(
                        pendingHolidays,
                        pendingTravel,
                        "Azure SQL unavailable: HTTP ${health.code()}"
                    )
                }

                val authorization =
                    "Bearer ${session.accessToken}"

                val holidays = count(
                    api.getHolidays(authorization),
                    "holidays"
                )

                val packingLists = count(
                    api.getPackingLists(authorization),
                    "packingLists"
                )

                val itineraries = count(
                    api.getItineraries(authorization),
                    "itineraries"
                )

                val checklists = count(
                    api.getChecklists(authorization),
                    "checklists"
                )

                SyncReadiness(
                    ready = true,
                    pendingHolidayChanges =
                        pendingHolidays,
                    pendingTravelChanges =
                        pendingTravel,
                    remoteHolidayCount =
                        holidays,
                    remotePackingListCount =
                        packingLists,
                    remoteItineraryCount =
                        itineraries,
                    remoteChecklistCount =
                        checklists,
                    message = "Azure synchronization is available."
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                unavailable(
                    pendingHolidays,
                    pendingTravel,
                    error.message
                        ?: "Azure is temporarily unavailable."
                )
            }
        }

    private fun count(
        response: Response<JsonObject>,
        field: String
    ): Int {
        if (!response.isSuccessful) {
            throw IllegalStateException(
                "API request failed: HTTP ${response.code()}"
            )
        }

        val body = response.body()
            ?: throw IllegalStateException(
                "Missing API response."
            )

        val array = body.getAsJsonArray(field)
            ?: throw IllegalStateException(
                "Missing $field in API response."
            )

        return array.size()
    }

    private fun unavailable(
        pendingHolidays: Int,
        pendingTravel: Int,
        message: String
    ) = SyncReadiness(
        ready = false,
        pendingHolidayChanges =
            pendingHolidays,
        pendingTravelChanges =
            pendingTravel,
        remoteHolidayCount = null,
        remotePackingListCount = null,
        remoteItineraryCount = null,
        remoteChecklistCount = null,
        message = message
    )
}
