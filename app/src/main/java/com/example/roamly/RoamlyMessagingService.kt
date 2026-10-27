
package com.example.roamly

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RoamlyMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "roamly_notifications"
        private const val TAG = "RoamlyFCM"
    }


    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)

        getSharedPreferences(
            "roamly_notifications",
            MODE_PRIVATE
        ).edit()
            .putString("firebase_installation_id", installationId)
            .apply()

        Log.d(TAG, "Firebase device registered successfully")
    }


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Firebase push message received")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Roamly"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        createNotificationChannel()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification
        )

        Log.d(TAG, "Notification displayed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Roamly Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Travel updates and reminders"
            }

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )

            notificationManager.createNotificationChannel(channel)
        }
    }
}
