
package com.example.roamly.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.roamly.BiometricAuthManager
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.auth.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class BiometricLockFragment : Fragment(R.layout.fragment_biometric_lock) {

    private lateinit var biometricAuthManager: BiometricAuthManager

    private var statusText: TextView? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        biometricAuthManager = BiometricAuthManager(requireActivity())

        statusText = view.findViewById(R.id.biometricStatusText)

        val retryButton =
            view.findViewById<MaterialButton>(
                R.id.biometricRetryButton
            )

        val googleButton =
            view.findViewById<MaterialButton>(
                R.id.biometricGoogleButton
            )

        retryButton.setOnClickListener {
            startBiometricAuthentication()
        }

        googleButton.setOnClickListener {
            useGoogleSignIn()
        }

        view.post {
            if (isAdded) {
                startBiometricAuthentication()
            }
        }
    }

    private fun startBiometricAuthentication() {
        biometricAuthManager.authenticate(
            onSuccess = {
                if (isAdded) {
                    val sessionManager = SessionManager(requireContext())

                    val hasFirebaseUser =
                        FirebaseAuth.getInstance().currentUser != null

                    if (hasFirebaseUser && sessionManager.hasSession()) {
                        (requireActivity() as MainActivity)
                            .showHomeAfterAuthentication()
                    } else {
                        (requireActivity() as MainActivity)
                            .showLogin()
                    }
                }
            },
            onError = { message ->
                statusText?.text = message
            },
            onUseGoogle = {
                useGoogleSignIn()
            }
        )
    }

    private fun useGoogleSignIn() {
        if (!isAdded) {
            return
        }

        biometricAuthManager.cancel()

        FirebaseAuth.getInstance().signOut()

        SessionManager(requireContext()).clearSession()

        (requireActivity() as MainActivity).showLogin()
    }

    override fun onDestroyView() {
        biometricAuthManager.cancel()
        statusText = null
        super.onDestroyView()
    }
}
