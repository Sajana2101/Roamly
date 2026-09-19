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

class RegisterFragment :
    Fragment(R.layout.fragment_register) {

    private val authRepository = AuthRepository()

    private val passwordPattern =
        Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
        )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        val nameLayout =
            view.findViewById<TextInputLayout>(
                R.id.registerNameLayout
            )

        val emailLayout =
            view.findViewById<TextInputLayout>(
                R.id.registerEmailLayout
            )

        val passwordLayout =
            view.findViewById<TextInputLayout>(
                R.id.registerPasswordLayout
            )

        val confirmPasswordLayout =
            view.findViewById<TextInputLayout>(
                R.id.registerConfirmPasswordLayout
            )

        val nameEditText =
            view.findViewById<TextInputEditText>(
                R.id.registerNameEditText
            )

        val emailEditText =
            view.findViewById<TextInputEditText>(
                R.id.registerEmailEditText
            )

        val passwordEditText =
            view.findViewById<TextInputEditText>(
                R.id.registerPasswordEditText
            )

        val confirmPasswordEditText =
            view.findViewById<TextInputEditText>(
                R.id.registerConfirmPasswordEditText
            )

        val registerButton =
            view.findViewById<MaterialButton>(
                R.id.registerButton
            )

        val openLoginButton =
            view.findViewById<MaterialButton>(
                R.id.openLoginButton
            )

        openLoginButton.setOnClickListener {
            (requireActivity() as MainActivity)
                .showLogin()
        }

        registerButton.setOnClickListener {
            nameLayout.error = null
            emailLayout.error = null
            passwordLayout.error = null
            confirmPasswordLayout.error = null

            val name =
                nameEditText.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val email =
                emailEditText.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val password =
                passwordEditText.text
                    ?.toString()
                    .orEmpty()

            val confirmPassword =
                confirmPasswordEditText.text
                    ?.toString()
                    .orEmpty()

            var valid = true

            if (name.isBlank()) {
                nameLayout.error =
                    getString(R.string.field_required)

                valid = false
            } else if (name.length < 2) {
                nameLayout.error =
                    getString(R.string.name_too_short)

                valid = false
            }

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
            } else if (
                !passwordPattern.matches(password)
            ) {
                passwordLayout.error =
                    getString(
                        R.string.password_requirements
                    )

                valid = false
            }

            if (confirmPassword.isBlank()) {
                confirmPasswordLayout.error =
                    getString(R.string.field_required)

                valid = false
            } else if (
                password != confirmPassword
            ) {
                confirmPasswordLayout.error =
                    getString(
                        R.string.passwords_do_not_match
                    )

                valid = false
            }

            if (!valid) {
                return@setOnClickListener
            }

            registerButton.isEnabled = false

            authRepository.register(
                name = name,
                email = email,
                password = password,
                confirmPassword = confirmPassword
            ).enqueue(
                object : Callback<AuthResponse> {

                    override fun onResponse(
                        call: Call<AuthResponse>,
                        response: Response<AuthResponse>
                    ) {
                        if (!isAdded) {
                            return
                        }

                        registerButton.isEnabled = true

                        if (response.isSuccessful) {
                            val authResponse =
                                response.body()

                            if (authResponse != null) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(
                                        R.string
                                            .registration_successful
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()

                                (
                                        requireActivity()
                                                as MainActivity
                                        )
                                    .showLogin()

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
                        if (!isAdded) {
                            return
                        }

                        registerButton.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.network_error
                            ),
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