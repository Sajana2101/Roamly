package com.example.roamly.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// it's the name of the old SharedPreferences file used by our Part 2 Settings screen
// by keeping this it allows existing settings to migrate into DataStore
private const val LEGACY_SETTINGS_NAME =
    "roamly_settings"

// DataStore uses its own file so the old SharedPreferences data can be
// migrated safely the first time the new Settings implementation is opened
private const val SETTINGS_DATASTORE_NAME =
    "roamly_settings_datastore"

// creates one shared DataStore instance for application settings,
// existing Part 2 preferences are automatically migrated when possible.
val Context.settingsDataStore:
        DataStore<Preferences> by preferencesDataStore(
    name =
        SETTINGS_DATASTORE_NAME,
    produceMigrations = {
            context ->

        listOf(
            SharedPreferencesMigration(
                context,
                LEGACY_SETTINGS_NAME
            )
        )
    }
)

class SettingsPreferencesRepository(
    private val dataStore:
    DataStore<Preferences>
) {

    private object PreferenceKeys {

        // this key deliberately matches the old SharedPreferences key
        // the previous language selection can still be migrated automatically
        val language =
            stringPreferencesKey(
                "language"
            )

        val homeCurrency =
            stringPreferencesKey(
                "home_currency"
            )

        // this also matches the old Part 2 key for migration
        val notificationsEnabled =
            booleanPreferencesKey(
                "notifications_enabled"
            )
    }

    // exposes the stored preferences as a Flow so the settings screen
    // can always work with the most recently saved values.
    val settings:
            Flow<SettingsPreferences> =
        dataStore.data.map {
                preferences ->

            SettingsPreferences(
                language =
                    preferences[
                        PreferenceKeys.language
                    ]
                        ?: SettingsPreferences.DEFAULT_LANGUAGE,

                homeCurrency =
                    preferences[
                        PreferenceKeys.homeCurrency
                    ]
                        ?: SettingsPreferences.DEFAULT_HOME_CURRENCY,

                notificationsEnabled =
                    preferences[
                        PreferenceKeys.notificationsEnabled
                    ]
                        ?: SettingsPreferences.DEFAULT_NOTIFICATIONS_ENABLED
            )
        }

    // this saves all user-configurable Settings values together.
    // DataStore performs this update as one asynchronous transaction
    suspend fun saveSettings(
        language: String,
        homeCurrency: String,
        notificationsEnabled: Boolean
    ) {

        dataStore.edit {
                preferences ->

            preferences[
                PreferenceKeys.language
            ] =
                language

            preferences[
                PreferenceKeys.homeCurrency
            ] =
                homeCurrency

            preferences[
                PreferenceKeys.notificationsEnabled
            ] =
                notificationsEnabled
        }
    }
}