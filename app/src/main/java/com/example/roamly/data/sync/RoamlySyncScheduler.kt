
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

    private val networkConstraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )
            .build()

    fun schedule(
        context: Context
    ) {
        val manager =
            WorkManager.getInstance(
                context.applicationContext
            )

        val periodicRequest =
            PeriodicWorkRequestBuilder<RoamlySyncWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(networkConstraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        manager.enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        requestNow(context)
    }

    fun requestNow(
        context: Context
    ) {
        val request =
            OneTimeWorkRequestBuilder<RoamlySyncWorker>()
                .setConstraints(networkConstraints)
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
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
