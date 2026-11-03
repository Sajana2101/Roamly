package com.example.roamly.data.model

// Represents the user-specific Settings values returned by the Roamly API.
data class RemoteUserSettings(
    val homeCurrency: String,
    val languageCode: String,
    val notificationsEnabled: Boolean,
    val updatedAt: String? = null
)

//Response wrapper returned by GET and PATCH /api/settings
data class UserSettingsResponse(
    val settings: RemoteUserSettings
)

//values that may be updated through PATCH /api/settings.
data class UpdateUserSettingsRequest(
    val homeCurrency: String,
    val languageCode: String,
    val notificationsEnabled: Boolean
)