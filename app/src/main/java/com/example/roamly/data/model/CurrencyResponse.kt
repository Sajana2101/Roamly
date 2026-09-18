package com.example.roamly.data.model

// Represents the completed conversion returned to the Currency screen.
// These fields match the response structure planned for the live currency endpoint.
data class CurrencyResponse(
    val from: String,
    val to: String,
    val amount: Double,
    val rate: Double,
    val convertedAmount: Double
)