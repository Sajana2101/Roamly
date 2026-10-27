
package com.example.roamly.data.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class ReminderRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val supportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )

        if (intent.action !in supportedActions) {
            return
        }

        Log.d(
            "RoamlyReminders",
            "Reschedule receiver started: ${intent.action}"
        )

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        try {
            val firebaseAuth = FirebaseAuth.getInstance()

            val listener = object : FirebaseAuth.AuthStateListener {

                override fun onAuthStateChanged(auth: FirebaseAuth) {
                    auth.removeAuthStateListener(this)

                    try {
                        if (auth.currentUser == null) {
                            Log.w(
                                "RoamlyReminders",
                                "No signed-in user available"
                            )
                            return
                        }

                        val reminders = ReminderLocalStore(appContext)
                            .getReminders()
                            .filter {
                                it.isEnabled &&
                                        it.scheduledAtMillis >
                                        System.currentTimeMillis()
                            }

                        Log.d(
                            "RoamlyReminders",
                            "Restoring ${reminders.size} reminders"
                        )

                        reminders.forEach { reminder ->
                            ReminderAlarmScheduler.schedule(
                                appContext,
                                reminder
                            )
                        }

                        Log.d(
                            "RoamlyReminders",
                            "Reminder restoration completed"
                        )
                    } catch (exception: Exception) {
                        Log.e(
                            "RoamlyReminders",
                            "Reminder restoration failed",
                            exception
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            firebaseAuth.addAuthStateListener(listener)
        } catch (exception: Exception) {
            Log.e(
                "RoamlyReminders",
                "Could not initialize reminder restoration",
                exception
            )

            pendingResult.finish()
        }
    }
}
