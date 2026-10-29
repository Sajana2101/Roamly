
package com.example.roamly.data.reminders

import android.content.Context
import com.example.roamly.data.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

class ReminderLocalStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "roamly_reminders",
            Context.MODE_PRIVATE
        )

    private fun getStorageKey(): String? {
        val userId = FirebaseAuth.getInstance()
            .currentUser?.uid ?: return null

        return "reminders_$userId"
    }

    fun getReminders(): List<Reminder> {
        val key = getStorageKey() ?: return emptyList()
        val storedData = preferences.getString(key, "[]") ?: "[]"

        return try {
            val jsonArray = JSONArray(storedData)
            val reminders = mutableListOf<Reminder>()

            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)

                reminders.add(
                    Reminder(
                        reminderId = item.getString("reminderId"),
                        title = item.getString("title"),
                        scheduledAtMillis = item.getLong("scheduledAtMillis"),
                        isEnabled = item.optBoolean("isEnabled", true),
                        description = item.optString("description", ""),
                        holidayId = if (
                            item.has("holidayId") &&
                            !item.isNull("holidayId")
                        ) {
                            item.getInt("holidayId")
                        } else {
                            null
                        }
                    )
                )
            }

            reminders.sortedBy { it.scheduledAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveReminders(reminders: List<Reminder>) {
        val key = getStorageKey() ?: return
        val jsonArray = JSONArray()

        reminders.forEach { reminder ->
            val item = JSONObject().apply {
                put("reminderId", reminder.reminderId)
                put("title", reminder.title)
                put("scheduledAtMillis", reminder.scheduledAtMillis)
                put("isEnabled", reminder.isEnabled)
                put("description", reminder.description)

                reminder.holidayId?.let {
                    put("holidayId", it)
                }
            }

            jsonArray.put(item)
        }

        preferences.edit()
            .putString(key, jsonArray.toString())
            .apply()
    }
}
