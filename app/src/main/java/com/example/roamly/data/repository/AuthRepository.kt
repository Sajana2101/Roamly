package com.example.roamly.data.repository

import com.example.roamly.data.model.AuthResponse
import com.example.roamly.data.model.GoogleAuthRequest
import com.example.roamly.data.model.LoginRequest
import com.example.roamly.data.model.RegisterRequest
import com.example.roamly.data.network.ApiService
import com.example.roamly.data.network.RetrofitClient
import retrofit2.Call

class AuthRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Call<AuthResponse> {
        val request = RegisterRequest(
            name = name,
            email = email,
            password = password,
            confirmPassword = confirmPassword
        )

        return apiService.register(request)
    }

    fun login(
        email: String,
        password: String
    ): Call<AuthResponse> {
        val request = LoginRequest(
            email = email,
            password = password
        )

        return apiService.login(request)
    }

    fun googleLogin(
        idToken: String
    ): Call<AuthResponse> {
        val request = GoogleAuthRequest(
            idToken = idToken
        )

        return apiService.googleLogin(request)
    }
}