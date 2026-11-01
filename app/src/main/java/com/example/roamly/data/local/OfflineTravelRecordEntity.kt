
package com.example.roamly.data.local

import androidx.room.Entity

@Entity(
    tableName = "offline_travel_records",
    primaryKeys = [
        "userId",
        "recordType",
        "recordId"
    ]
)
data class OfflineTravelRecordEntity(
    val userId: String,
    val recordType: String,
    val recordId: String,
    val dataJson: String,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)
