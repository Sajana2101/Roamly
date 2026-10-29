
package com.example.roamly.data.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.roamly.data.model.Reminder
import com.google.firebase.auth.FirebaseAuth

object ReminderAlarmScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        cancel(context, reminder.reminderId)

        if (
            !reminder.isEnabled ||
            reminder.scheduledAtMillis <= System.currentTimeMillis()
        ) {
            return
        }

        val userId = FirebaseAuth.getInstance()
            .currentUser?.uid ?: return

        val intent = createIntent(context, reminder.reminderId).apply {
            putExtra("reminder_id", reminder.reminderId)
            putExtra("user_id", userId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            AlarmManager::class.java
        )

        val canScheduleExact =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.scheduledAtMillis,
                    pendingIntent
                )
                return
            } catch (_: SecurityException) {
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminder.scheduledAtMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context, reminderId: String) {
        val intent = createIntent(context, reminderId)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val alarmManager = context.getSystemService(
            AlarmManager::class.java
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createIntent(
        context: Context,
        reminderId: String
    ): Intent {
        return Intent(
            context,
            ReminderAlarmReceiver::class.java
        ).apply {
            data = Uri.parse(
                "roamly://reminder/${Uri.encode(reminderId)}"
            )
        }
    }
}
