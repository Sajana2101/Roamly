
package com.example.roamly

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val activity: FragmentActivity
) {

    private var onAuthenticated: () -> Unit = {}
    private var onFailure: (String) -> Unit = {}
    private var onGoogleSignIn: () -> Unit = {}

    private val biometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()

                onFailure(
                    activity.getString(R.string.biometric_failed)
                )
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)

                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onGoogleSignIn()
                } else {
                    onFailure(errString.toString())
                }
            }
        }
    )

    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUseGoogle: () -> Unit
    ) {
        onAuthenticated = onSuccess
        onFailure = onError
        onGoogleSignIn = onUseGoogle

        val biometricManager = BiometricManager.from(activity)

        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            onFailure(
                activity.getString(R.string.biometric_unavailable)
            )
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(
                activity.getString(R.string.biometric_unlock_title)
            )
            .setSubtitle(
                activity.getString(R.string.biometric_unlock_message)
            )
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText(
                activity.getString(R.string.biometric_use_google)
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun cancel() {
        biometricPrompt.cancelAuthentication()
    }
}
