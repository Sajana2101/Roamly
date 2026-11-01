
package com.example.roamly.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.network.ApiConfig
import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RoamlySyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val sessions =
        SessionManager(applicationContext)

    private val api =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SyncApiService::class.java)

    override suspend fun doWork(): Result {
        val session = sessions.getSession()
            ?: return Result.success()

        return try {
            val health = api.checkDatabase()

            if (!health.isSuccessful) {
                Log.w(
                    TAG,
                    "Azure database unavailable: ${health.code()}"
                )

                return Result.retry()
            }

            val currentUser =
                sessions.getSession()?.userId

            if (currentUser != session.userId) {
                return Result.success()
            }

            if (!ENABLE_REMOTE_WRITES) {
                Log.i(
                    TAG,
                    "Background sync ready; uploads awaiting reconciliation"
                )

                return Result.success()
            }

            val holidays =
                HolidaySyncUploader(
                    applicationContext
                ).uploadPending()

            if (!holidays.success) {
                Log.w(
                    TAG,
                    "Holiday sync incomplete: ${holidays.message}"
                )

                return Result.retry()
            }

            if (
                sessions.getSession()?.userId !=
                session.userId
            ) {
                return Result.success()
            }

            val travel =
                TravelSyncUploader(
                    applicationContext
                ).uploadPending()

            if (!travel.success) {
                Log.w(
                    TAG,
                    "Travel sync incomplete: ${travel.message}"
                )

                return Result.retry()
            }

            Log.i(
                TAG,
                "Background sync complete"
            )

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(
                TAG,
                "Background sync failed",
                error
            )

            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RoamlySync"
        private const val ENABLE_REMOTE_WRITES = false
    }
}
