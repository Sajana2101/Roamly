package com.example.roamly.data.model

data class Holiday(
    val holidayId: Int,
    val name: String,
    val location: String,
    val startDate: String,
    val endDate: String,
    val tripType: String = "",
    val country: String = "",
    val city: String = "",
    val coverImageUri: String? = null
) {
    val title: String
        get() = name
}