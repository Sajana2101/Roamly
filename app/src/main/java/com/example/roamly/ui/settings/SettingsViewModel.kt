package com.example.roamly.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roamly.data.settings.SettingsPreferences
import com.example.roamly.data.settings.SettingsPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository:
    SettingsPreferencesRepository
) : ViewModel() {

    //  the latest saved Settings values are kept available to the UI
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

    // saves the selected preferences without blocking the Settings screen.
    fun saveSettings(
        language: String,
        homeCurrency: String,
        notificationsEnabled: Boolean
    ) {

        viewModelScope.launch {

            repository.saveSettings(
                language =
                    language,
                homeCurrency =
                    homeCurrency,
                notificationsEnabled =
                    notificationsEnabled
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