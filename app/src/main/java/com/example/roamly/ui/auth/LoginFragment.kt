package com.example.roamly.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.model.ApiError
import com.example.roamly.data.model.AuthResponse
import com.example.roamly.data.repository.AuthRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val authRepository = AuthRepository()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        val emailLayout =
            view.findViewById<TextInputLayout>(
                R.id.loginEmailLayout
            )

        val passwordLayout =
            view.findViewById<TextInputLayout>(
                R.id.loginPasswordLayout
            )

        val emailEditText =
            view.findViewById<TextInputEditText>(
                R.id.loginEmailEditText
            )

        val passwordEditText =
            view.findViewById<TextInputEditText>(
                R.id.loginPasswordEditText
            )

        val loginButton =
            view.findViewById<MaterialButton>(
                R.id.loginButton
            )

        val openRegisterButton =
            view.findViewById<MaterialButton>(
                R.id.openRegisterButton
            )

        openRegisterButton.setOnClickListener {
            (requireActivity() as MainActivity)
                .showRegister()
        }

        loginButton.setOnClickListener {
            emailLayout.error = null
            passwordLayout.error = null

            val email =
                emailEditText.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val password =
                passwordEditText.text
                    ?.toString()
                    .orEmpty()

            var valid = true

            if (email.isBlank()) {
                emailLayout.error =
                    getString(R.string.field_required)

                valid = false
            } else if (
                !Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {
                emailLayout.error =
                    getString(R.string.invalid_email)

                valid = false
            }

            if (password.isBlank()) {
                passwordLayout.error =
                    getString(R.string.field_required)

                valid = false
            }

            if (!valid) {
                return@setOnClickListener
            }

            loginButton.isEnabled = false

            authRepository.login(
                email = email,
                password = password
            ).enqueue(
                object : Callback<AuthResponse> {

                    override fun onResponse(
                        call: Call<AuthResponse>,
                        response: Response<AuthResponse>
                    ) {
                        if (!isAdded) {
                            return
                        }

                        loginButton.isEnabled = true

                        if (response.isSuccessful) {
                            val authResponse =
                                response.body()

                            if (authResponse != null) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(
                                        R.string.login_successful
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()

                                (
                                        requireActivity()
                                                as MainActivity
                                        )
                                    .showHomeAfterAuthentication()

                                return
                            }
                        }

                        Toast.makeText(
                            requireContext(),
                            getApiError(response),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onFailure(
                        call: Call<AuthResponse>,
                        throwable: Throwable
                    ) {
                        Log.e(
                            "RoamlyAuth",
                            "Login request failed",
                            throwable
                        )

                        if (!isAdded) {
                            return
                        }

                        loginButton.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.network_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
    }

    private fun getApiError(
        response: Response<*>
    ): String {
        val errorBody =
            response.errorBody()?.string()
                ?: return getString(
                    R.string.authentication_error
                )

        return try {
            Gson()
                .fromJson(
                    errorBody,
                    ApiError::class.java
                )
                .message
        } catch (_: Exception) {
            getString(
                R.string.authentication_error
            )
        }
    }
}