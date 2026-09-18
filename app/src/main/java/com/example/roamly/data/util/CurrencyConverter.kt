package com.example.roamly.data.util


// This keeps the local calculation logic separate from the Fragment and ViewModel.
// It also makes the conversion calculations easier to unit test independently.
object CurrencyConverter {

    // Calculates the direct exchange rate between two currencies
    // im using ZAR as the common base for the local prototype.
    fun calculateRate(
        fromRateToZar: Double,
        toRateToZar: Double
    ): Double {

        require(fromRateToZar > 0.0) {
            "Source currency rate must be greater than zero."
        }

        require(toRateToZar > 0.0) {
            "Destination currency rate must be greater than zero."
        }

        return fromRateToZar / toRateToZar
    }

    // Performs the local currency conversion.
    // This will later be replaced with the value returned by the Roamly REST API.
    fun convert(
        amount: Double,
        fromRateToZar: Double,
        toRateToZar: Double
    ): Double {

        require(amount.isFinite() && amount > 0.0) {
            "Amount must be greater than zero."
        }

        val rate =
            calculateRate(
                fromRateToZar,
                toRateToZar
            )

        return amount * rate
    }
}