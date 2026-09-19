package com.example.roamly.data.model

data class AuthUser(
    val userId: String,
    val name: String,
    val email: String,
    val ssoProvider: String?
)