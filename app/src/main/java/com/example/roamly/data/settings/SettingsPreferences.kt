package com.example.roamly.data.settings

// stores the small user preferences that belong to the Settings screen.
// these values are kept separately from authentication /. session information.
data class SettingsPreferences(
    val language: String = DEFAULT_LANGUAGE,
    val homeCurrency: String = DEFAULT_HOME_CURRENCY,
    val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS_ENABLED
) {

    companion object {

        const val DEFAULT_LANGUAGE =
            "English"

        const val DEFAULT_HOME_CURRENCY =
            "ZAR"

        const val DEFAULT_NOTIFICATIONS_ENABLED =
            true
    }
}