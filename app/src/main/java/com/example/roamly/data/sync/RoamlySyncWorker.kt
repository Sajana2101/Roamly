
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

            if (
                sessions.getSession()?.userId !=
                session.userId
            ) {
                return Result.success()
            }

            val reconciliation =
                SyncAttemptReconciler(
                    applicationContext
                ).reconcile()

            if (reconciliation.unresolved > 0) {
                Log.w(
                    TAG,
                    "${reconciliation.unresolved} creation attempts require review"
                )

                return Result.failure()
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
                "Background synchronization completed"
            )

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(
                TAG,
                "Background synchronization failed",
                error
            )

            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RoamlySync"
    }
}
