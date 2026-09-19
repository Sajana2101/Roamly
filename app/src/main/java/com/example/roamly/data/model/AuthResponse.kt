package com.example.roamly.data.model

data class AuthResponse(
    val token: String,
    val user: AuthUser
)