package com.example.roamly.data.auth

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.FragmentActivity
import com.example.roamly.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInHelper(
    private val activity: FragmentActivity
) {

    private val credentialManager =
        CredentialManager.create(
            activity
        )

    suspend fun getGoogleIdToken(): String {

        val googleSignInOption =
            GetSignInWithGoogleOption.Builder(
                serverClientId =
                    activity.getString(
                        R.string.default_web_client_id
                    )
            )
                .build()

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    googleSignInOption
                )
                .build()

        val result =
            credentialManager.getCredential(
                context = activity,
                request = request
            )

        val credential =
            result.credential

        if (
            credential is CustomCredential &&
            credential.type ==
            GoogleIdTokenCredential
                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            val googleCredential =
                GoogleIdTokenCredential
                    .createFrom(
                        credential.data
                    )

            return googleCredential.idToken
        }

        throw IllegalStateException(
            "Google sign-in did not return a Google ID token."
        )
    }
}