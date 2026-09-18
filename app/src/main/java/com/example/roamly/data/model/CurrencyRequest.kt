package com.example.roamly.data.model

// Represents the values required to perform a currency conversion
// The same structure can later be used when these values are sent to the REST API
data class CurrencyRequest(
    val from: String,
    val to: String,
    val amount: Double
)