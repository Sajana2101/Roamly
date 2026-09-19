package com.example.roamly.data.network

import com.example.roamly.data.model.AuthResponse
import com.example.roamly.data.model.GoogleAuthRequest
import com.example.roamly.data.model.LoginRequest
import com.example.roamly.data.model.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<AuthResponse>

    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<AuthResponse>

    @POST("api/auth/google")
    fun googleLogin(
        @Body request: GoogleAuthRequest
    ): Call<AuthResponse>
}