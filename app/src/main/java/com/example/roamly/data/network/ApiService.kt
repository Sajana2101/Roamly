package com.example.roamly.data.network

import com.example.roamly.data.model.AuthResponse
import com.example.roamly.data.model.GoogleAuthRequest
import com.example.roamly.data.model.LoginRequest
import com.example.roamly.data.model.RegisterRequest
import com.example.roamly.data.model.UpdateUserSettingsRequest
import com.example.roamly.data.model.UserSettingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
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

    //Retrieves the Settings stored for the currently authenticated user
    @GET("api/settings")
    suspend fun getSettings(
        @Header("Authorization")
        authorization: String
    ): Response<UserSettingsResponse>

    ///Updates the Settings stored for the currently authenticated user
    @PATCH("api/settings")
    suspend fun updateSettings(
        @Header("Authorization")
        authorization: String,
        @Body
        request: UpdateUserSettingsRequest
    ): Response<UserSettingsResponse>
}