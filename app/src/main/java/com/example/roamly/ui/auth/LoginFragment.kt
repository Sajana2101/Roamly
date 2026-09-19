package com.example.roamly.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.auth.GoogleSignInHelper
import com.example.roamly.data.auth.RoamlyAuthRepository
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginFragment :
    Fragment(
        R.layout.fragment_login
    ) {

    private lateinit var firebaseAuth:
            FirebaseAuth

    private lateinit var googleSignInHelper:
            GoogleSignInHelper

    private lateinit var roamlyAuthRepository:
            RoamlyAuthRepository

    private lateinit var googleSignInButton:
            MaterialButton

    private lateinit var progressBar:
            ProgressBar

    private lateinit var statusTextView:
            TextView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        firebaseAuth =
            FirebaseAuth.getInstance()

        googleSignInHelper =
            GoogleSignInHelper(
                requireActivity()
            )

        roamlyAuthRepository =
            RoamlyAuthRepository()

        googleSignInButton =
            view.findViewById(
                R.id.googleSignInButton
            )

        progressBar =
            view.findViewById(
                R.id.googleSignInProgressBar
            )

        statusTextView =
            view.findViewById(
                R.id.googleSignInStatusTextView
            )

        googleSignInButton
            .setOnClickListener {

                startGoogleSignIn()
            }
    }

    private fun startGoogleSignIn() {

        setLoading(
            true
        )

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                try {

                    val googleIdToken =
                        googleSignInHelper
                            .getGoogleIdToken()

                    authenticateWithFirebase(
                        googleIdToken
                    )

                } catch (
                    exception:
                    GetCredentialException
                ) {

                    Log.w(
                        TAG,
                        "Google credential request failed",
                        exception
                    )

                    setLoading(
                        false
                    )

                    if (isAdded) {

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string
                                    .firebase_google_cancelled
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (
                    exception:
                    Exception
                ) {

                    Log.e(
                        TAG,
                        "Google sign-in failed",
                        exception
                    )

                    setLoading(
                        false
                    )

                    if (isAdded) {

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string
                                    .firebase_google_failed
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    private fun authenticateWithFirebase(
        googleIdToken: String
    ) {

        val firebaseCredential =
            GoogleAuthProvider
                .getCredential(
                    googleIdToken,
                    null
                )

        firebaseAuth
            .signInWithCredential(
                firebaseCredential
            )
            .addOnCompleteListener {
                    task ->

                if (!isAdded) {

                    return@addOnCompleteListener
                }

                if (
                    task.isSuccessful
                ) {

                    val firebaseUser =
                        firebaseAuth
                            .currentUser

                    Log.i(
                        TAG,
                        "Firebase authentication succeeded. " +
                                "UID=${firebaseUser?.uid}, " +
                                "email=${firebaseUser?.email}"
                    )

                    authenticateWithRoamlyBackend(
                        googleIdToken
                    )

                } else {

                    setLoading(
                        false
                    )

                    Log.e(
                        TAG,
                        "Firebase Google authentication failed",
                        task.exception
                    )

                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string
                                .firebase_google_failed
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun authenticateWithRoamlyBackend(
        googleIdToken: String
    ) {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                try {

                    statusTextView.text =
                        getString(
                            R.string
                                .roamly_connecting_account
                        )

                    val authResponse =
                        roamlyAuthRepository
                            .authenticateWithGoogle(
                                googleIdToken
                            )

                    Log.i(
                        TAG,
                        "Roamly backend authentication succeeded. " +
                                "userId=${authResponse.user.userId}, " +
                                "email=${authResponse.user.email}, " +
                                "provider=${authResponse.user.ssoProvider}"
                    )

                    setLoading(
                        false
                    )

                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string
                                .roamly_sign_in_successful
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    (
                            requireActivity()
                                    as MainActivity
                            )
                        .showHomeAfterAuthentication()

                } catch (
                    exception:
                    Exception
                ) {

                    Log.e(
                        TAG,
                        "Roamly backend authentication failed",
                        exception
                    )

                    firebaseAuth
                        .signOut()

                    setLoading(
                        false
                    )

                    if (isAdded) {

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string
                                    .roamly_backend_failed
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    private fun setLoading(
        loading: Boolean
    ) {

        googleSignInButton
            .isEnabled =
            !loading

        progressBar.visibility =
            if (loading) {

                View.VISIBLE

            } else {

                View.GONE
            }

        statusTextView.visibility =
            if (loading) {

                View.VISIBLE

            } else {

                View.GONE
            }

        if (!loading) {

            statusTextView.text =
                getString(
                    R.string.firebase_signing_in
                )
        }
    }

    companion object {

        private const val TAG =
            "RoamlyGoogleAuth"
    }
}