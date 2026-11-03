package com.example.roamly.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.settings.SettingsLanguageMapper
import com.example.roamly.data.settings.SettingsPreferences
import com.example.roamly.data.settings.SettingsPreferencesRepository
import com.example.roamly.data.settings.SettingsRemoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository:
    SettingsPreferencesRepository,
    private val remoteRepository:
    SettingsRemoteRepository =
        SettingsRemoteRepository()
) : ViewModel() {

    companion object {

        private const val TAG =
            "SettingsViewModel"
    }

    //keeps the latest locally stored Settings available to the UI
    val settings:
            StateFlow<SettingsPreferences> =
        repository.settings.stateIn(
            scope =
                viewModelScope,
            started =
                SharingStarted.WhileSubscribed(
                    5_000
                ),
            initialValue =
                SettingsPreferences()
        )

    // Synchronises the local Settings with the authenticated cloud account.
    fun refreshFromCloud(
        accessToken: String?
    ) {

        if (
            accessToken.isNullOrBlank()
        ) {

            return
        }

        viewModelScope.launch {

            try {

                val remoteSettings =
                    remoteRepository
                        .getSettings(
                            accessToken
                        )

                if (
                    remoteSettings.updatedAt ==
                    null
                ) {

                    // The API has no saved Settings record yet
                    //Preserve the user's existing local preferences
                    //and create the first cloud copy from them.
                    val localSettings =
                        repository
                            .settings
                            .first()

                    remoteRepository
                        .updateSettings(
                            accessToken =
                                accessToken,
                            homeCurrency =
                                localSettings
                                    .homeCurrency,
                            language =
                                localSettings
                                    .language,
                            notificationsEnabled =
                                localSettings
                                    .notificationsEnabled
                        )

                    Log.d(
                        TAG,
                        "Initial cloud Settings record created."
                    )

                } else {

                    // existing cloud settings belong to the signed-in user
                    // so it's copied into DataStore for local/offline use
                    repository
                        .saveSettings(
                            language =
                                SettingsLanguageMapper
                                    .toDisplayName(
                                        remoteSettings
                                            .languageCode
                                    ),
                            homeCurrency =
                                remoteSettings
                                    .homeCurrency,
                            notificationsEnabled =
                                remoteSettings
                                    .notificationsEnabled
                        )

                    Log.d(
                        TAG,
                        "Settings restored from cloud."
                    )
                }

            } catch (
                exception: Exception
            ) {

                //Local DataStore Settings remain available if the API
                // cannot be reached
                Log.w(
                    TAG,
                    "Cloud Settings refresh failed. " +
                            "Using local preferences.",
                    exception
                )
            }
        }
    }

    // Saves locally first, then attempts to update the signed-in cloud account
    fun saveSettings(
        language: String,
        homeCurrency: String,
        notificationsEnabled: Boolean,
        accessToken: String?,
        onCompleted:
            (cloudSaved: Boolean) -> Unit
    ) {

        viewModelScope.launch {

            repository
                .saveSettings(
                    language =
                        language,
                    homeCurrency =
                        homeCurrency,
                    notificationsEnabled =
                        notificationsEnabled
                )

            if (
                accessToken.isNullOrBlank()
            ) {

                onCompleted(
                    false
                )

                return@launch
            }

            val cloudSaved =
                try {

                    remoteRepository
                        .updateSettings(
                            accessToken =
                                accessToken,
                            homeCurrency =
                                homeCurrency,
                            language =
                                language,
                            notificationsEnabled =
                                notificationsEnabled
                        )

                    Log.d(
                        TAG,
                        "Settings saved locally and to cloud."
                    )

                    true

                } catch (
                    exception: Exception
                ) {

                    Log.w(
                        TAG,
                        "Settings saved locally, " +
                                "but cloud update failed.",
                        exception
                    )

                    false
                }

            onCompleted(
                cloudSaved
            )
        }
    }

    class Factory(
        private val repository:
        SettingsPreferencesRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {

            if (
                modelClass.isAssignableFrom(
                    SettingsViewModel::class.java
                )
            ) {

                return SettingsViewModel(
                    repository
                ) as T
            }

            throw IllegalArgumentException(
                "Unknown ViewModel class."
            )
        }
    }
}