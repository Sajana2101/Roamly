package com.example.roamly.data.model

data class ItineraryActivity(
    val activityId: Int,
    val dayNumber: Int,
    val title: String,
    val startTime: String,
    val endTime: String? = null,
    val location: String? = null,
    val notes: String? = null
)