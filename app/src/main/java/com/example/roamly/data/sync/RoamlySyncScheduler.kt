
package com.example.roamly.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RoamlySyncScheduler {

    private const val PERIODIC_NAME =
        "roamly_background_sync"

    private const val IMMEDIATE_NAME =
        "roamly_connection_sync"

    private val constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun schedule(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<RoamlySyncWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        WorkManager.getInstance(
            context.applicationContext
        ).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        requestNow(context)
    }

    fun requestNow(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<RoamlySyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        WorkManager.getInstance(
            context.applicationContext
        ).enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )

        PackingRestoreWorker.request(context)
        DocumentSyncWorker.request(context)
    }
}
