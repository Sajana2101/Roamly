
package com.example.roamly.data.model

data class Reminder(
    val reminderId: String,
    val title: String,
    val scheduledAtMillis: Long,
    val isEnabled: Boolean = true,
    val description: String = "",
    val holidayId: Int? = null
)
