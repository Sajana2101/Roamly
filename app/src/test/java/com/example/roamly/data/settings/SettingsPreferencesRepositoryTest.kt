package com.example.roamly.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class SettingsPreferencesRepositoryTest {

    @get:Rule
    val temporaryFolder =
        TemporaryFolder()

    private fun createRepository():
            SettingsPreferencesRepository {

        val preferencesFile =
            File(
                temporaryFolder.root,
                "${UUID.randomUUID()}.preferences_pb"
            )

        val dataStore =
            PreferenceDataStoreFactory.create(
                produceFile = {
                    preferencesFile
                }
            )

        return SettingsPreferencesRepository(
            dataStore
        )
    }

    @Test
    fun defaultSettings_areReturnedWhenNothingHasBeenSaved() =
        runBlocking {

            val repository =
                createRepository()

            val settings =
                repository
                    .settings
                    .first()

            assertEquals(
                "English",
                settings.language
            )

            assertEquals(
                "ZAR",
                settings.homeCurrency
            )

            assertTrue(
                settings.notificationsEnabled
            )
        }

    @Test
    fun saveSettings_persistsAllSelectedPreferences() =
        runBlocking {

            val repository =
                createRepository()

            repository.saveSettings(
                language =
                    "Afrikaans",
                homeCurrency =
                    "EUR",
                notificationsEnabled =
                    false
            )

            val savedSettings =
                repository
                    .settings
                    .first()

            assertEquals(
                "Afrikaans",
                savedSettings.language
            )

            assertEquals(
                "EUR",
                savedSettings.homeCurrency
            )

            assertFalse(
                savedSettings.notificationsEnabled
            )
        }

    @Test
    fun saveSettings_canPersistEnabledNotifications() =
        runBlocking {

            val repository =
                createRepository()

            repository.saveSettings(
                language =
                    "English",
                homeCurrency =
                    "ZAR",
                notificationsEnabled =
                    true
            )

            val savedSettings =
                repository
                    .settings
                    .first()

            assertTrue(
                savedSettings.notificationsEnabled
            )
        }

    @Test
    fun saveSettings_persistsAlternativePreferenceCombination() =
        runBlocking {

            val repository =
                createRepository()

            repository.saveSettings(
                language =
                    "isiZulu",
                homeCurrency =
                    "GBP",
                notificationsEnabled =
                    false
            )

            val savedSettings =
                repository
                    .settings
                    .first()

            assertEquals(
                "isiZulu",
                savedSettings.language
            )

            assertEquals(
                "GBP",
                savedSettings.homeCurrency
            )

            assertFalse(
                savedSettings.notificationsEnabled
            )
        }
}