package com.example.roamly.data.settings

import com.example.roamly.data.model.RemoteUserSettings
import com.example.roamly.data.model.UpdateUserSettingsRequest
import com.example.roamly.data.network.ApiService
import com.example.roamly.data.network.RetrofitClient

// Handles Settings communication with the authenticated Roamly REST API
class SettingsRemoteRepository(
    private val apiService:
    ApiService =
        RetrofitClient.apiService
) {

    suspend fun getSettings(
        accessToken: String
    ): RemoteUserSettings {

        val response =
            apiService.getSettings(
                createAuthorizationHeader(
                    accessToken
                )
            )

        if (
            !response.isSuccessful
        ) {

            throw IllegalStateException(
                "Unable to retrieve settings. " +
                        "HTTP ${response.code()}"
            )
        }

        return response
            .body()
            ?.settings
            ?: throw IllegalStateException(
                "The Settings API returned an empty response."
            )
    }

    suspend fun updateSettings(
        accessToken: String,
        homeCurrency: String,
        language: String,
        notificationsEnabled: Boolean
    ): RemoteUserSettings {

        val request =
            UpdateUserSettingsRequest(
                homeCurrency =
                    homeCurrency,
                languageCode =
                    SettingsLanguageMapper
                        .toLanguageCode(
                            language
                        ),
                notificationsEnabled =
                    notificationsEnabled
            )

        val response =
            apiService.updateSettings(
                authorization =
                    createAuthorizationHeader(
                        accessToken
                    ),
                request =
                    request
            )

        if (
            !response.isSuccessful
        ) {

            throw IllegalStateException(
                "Unable to update settings. " +
                        "HTTP ${response.code()}"
            )
        }

        return response
            .body()
            ?.settings
            ?: throw IllegalStateException(
                "The Settings API returned an empty response."
            )
    }

    private fun createAuthorizationHeader(
        accessToken: String
    ): String {

        return if (
            accessToken.startsWith(
                "Bearer ",
                ignoreCase = true
            )
        ) {

            accessToken

        } else {

            "Bearer $accessToken"
        }
    }
}