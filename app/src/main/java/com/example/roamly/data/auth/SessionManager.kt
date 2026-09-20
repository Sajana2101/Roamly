package com.example.roamly.data.auth

import android.content.Context

data class RoamlySession(
    val accessToken: String,
    val userId: String,
    val name: String,
    val email: String,
    val ssoProvider: String?
)

class SessionManager(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    fun saveSession(
        accessToken: String,
        user: RoamlyAuthUser
    ) {

        preferences
            .edit()
            .putString(
                KEY_ACCESS_TOKEN,
                accessToken
            )
            .putString(
                KEY_USER_ID,
                user.userId
            )
            .putString(
                KEY_NAME,
                user.name
            )
            .putString(
                KEY_EMAIL,
                user.email
            )
            .putString(
                KEY_SSO_PROVIDER,
                user.ssoProvider
            )
            .apply()
    }

    fun hasSession(): Boolean {

        val accessToken =
            preferences.getString(
                KEY_ACCESS_TOKEN,
                null
            )

        val userId =
            preferences.getString(
                KEY_USER_ID,
                null
            )

        val email =
            preferences.getString(
                KEY_EMAIL,
                null
            )

        return !accessToken.isNullOrBlank() &&
                !userId.isNullOrBlank() &&
                !email.isNullOrBlank()
    }

    fun getSession(): RoamlySession? {

        if (!hasSession()) {
            return null
        }

        val accessToken =
            preferences.getString(
                KEY_ACCESS_TOKEN,
                null
            ) ?: return null

        val userId =
            preferences.getString(
                KEY_USER_ID,
                null
            ) ?: return null

        val name =
            preferences.getString(
                KEY_NAME,
                ""
            ).orEmpty()

        val email =
            preferences.getString(
                KEY_EMAIL,
                null
            ) ?: return null

        val ssoProvider =
            preferences.getString(
                KEY_SSO_PROVIDER,
                null
            )

        return RoamlySession(
            accessToken = accessToken,
            userId = userId,
            name = name,
            email = email,
            ssoProvider = ssoProvider
        )
    }

    fun getAccessToken(): String? {

        return preferences.getString(
            KEY_ACCESS_TOKEN,
            null
        )
    }

    fun getUserId(): String? {

        return preferences.getString(
            KEY_USER_ID,
            null
        )
    }

    fun getName(): String? {

        return preferences.getString(
            KEY_NAME,
            null
        )
    }

    fun getEmail(): String? {

        return preferences.getString(
            KEY_EMAIL,
            null
        )
    }

    fun updateName(
        name: String
    ) {

        preferences
            .edit()
            .putString(
                KEY_NAME,
                name
            )
            .apply()
    }

    fun clearSession() {

        preferences
            .edit()
            .clear()
            .apply()
    }

    companion object {

        private const val PREFERENCES_NAME =
            "roamly_auth_session"

        private const val KEY_ACCESS_TOKEN =
            "access_token"

        private const val KEY_USER_ID =
            "user_id"

        private const val KEY_NAME =
            "name"

        private const val KEY_EMAIL =
            "email"

        private const val KEY_SSO_PROVIDER =
            "sso_provider"
    }
}