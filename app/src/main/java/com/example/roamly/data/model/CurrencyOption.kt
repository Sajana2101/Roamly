package com.example.roamly.data.model

// Stores the information needed to display and calculate with each supported currency.
// rateToZar is only being used for local prototype calculations until the live API is connected.
data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String,
    val rateToZar: Double
) {

    // Makes each dropdown option easy to read instead of showing only the currency code.
    override fun toString(): String {
        return "$code — $name"
    }
}