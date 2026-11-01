
package com.example.roamly.data.local

import androidx.room.Entity

@Entity(
    tableName = "offline_holidays",
    primaryKeys = ["userId", "holidayId"]
)
data class OfflineHolidayEntity(
    val userId: String,
    val holidayId: Int,
    val name: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val holidayJson: String,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)
