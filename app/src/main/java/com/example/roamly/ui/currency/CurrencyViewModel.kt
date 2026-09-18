package com.example.roamly.ui.currency

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.roamly.data.model.CurrencyOption
import com.example.roamly.data.model.CurrencyRequest
import com.example.roamly.data.model.CurrencyResponse
import com.example.roamly.data.util.CurrencyConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


// This groups the values that the Currency screen needs to display at any point in time.
// By keeping them together, it makes it easier for the fragment to react to state changes.
data class CurrencyUiState(
    val fromCurrency: CurrencyOption,
    val toCurrency: CurrencyOption,
    val result: CurrencyResponse? = null,
    val errorMessage: String? = null
)

class CurrencyViewModel : ViewModel() {

    companion object {
        private const val TAG =
            "CurrencyViewModel"
    }

    // LOCAL PROTOTYPE DATA
    // These temporary rates let the full feature work until the REST API is ready.
    // Each value represents the approximate ZAR value of one unit of that currency.
    val currencies = listOf(
        CurrencyOption(
            code = "ZAR",
            name = "South African Rand",
            symbol = "R",
            rateToZar = 1.0
        ),
        CurrencyOption(
            code = "USD",
            name = "US Dollar",
            symbol = "$",
            rateToZar = 17.50
        ),
        CurrencyOption(
            code = "EUR",
            name = "Euro",
            symbol = "€",
            rateToZar = 20.00
        ),
        CurrencyOption(
            code = "GBP",
            name = "British Pound",
            symbol = "£",
            rateToZar = 23.50
        ),
        CurrencyOption(
            code = "AED",
            name = "UAE Dirham",
            symbol = "AED ",
            rateToZar = 4.76
        ),
        CurrencyOption(
            code = "JPY",
            name = "Japanese Yen",
            symbol = "¥",
            rateToZar = 0.12
        ),
        CurrencyOption(
            code = "AUD",
            name = "Australian Dollar",
            symbol = "A$",
            rateToZar = 11.70
        ),
        CurrencyOption(
            code = "CAD",
            name = "Canadian Dollar",
            symbol = "C$",
            rateToZar = 12.80
        )
    )

    // ZAR to EUR matches the example shown in the Part 1 prototype.
    private val defaultFromCurrency =
        currencies.first {
            it.code == "ZAR"
        }

    private val defaultToCurrency =
        currencies.first {
            it.code == "EUR"
        }

    // Stores the state currently displayed by the Currency screen.
    // Holds the current Currency screen state privately so it can only be changed through this ViewModel.
    private val _uiState =
        MutableStateFlow(
            CurrencyUiState(
                fromCurrency =
                    defaultFromCurrency,
                toCurrency =
                    defaultToCurrency
            )
        )

    val uiState:
            StateFlow<CurrencyUiState> =
        _uiState

    // Updates the source currency and clears any result that was calculated using the old selection.
    fun selectFromCurrency(
        currency: CurrencyOption
    ) {

        _uiState.value =
            _uiState.value.copy(
                fromCurrency = currency,
                result = null,
                errorMessage = null
            )

        Log.d(
            TAG,
            "Source currency changed to ${currency.code}"
        )
    }

    // Updates the destination currency and clears the previous conversion result
    fun selectToCurrency(
        currency: CurrencyOption
    ) {

        _uiState.value =
            _uiState.value.copy(
                toCurrency = currency,
                result = null,
                errorMessage = null
            )

        Log.d(
            TAG,
            "Destination currency changed to ${currency.code}"
        )
    }

    // Reverses the source and destination currencies without needing the user to select them again
    fun swapCurrencies() {

        val currentState =
            _uiState.value

        _uiState.value =
            currentState.copy(
                fromCurrency =
                    currentState.toCurrency,
                toCurrency =
                    currentState.fromCurrency,
                result = null,
                errorMessage = null
            )

        Log.d(
            TAG,
            "Currencies swapped to " +
                    "${_uiState.value.fromCurrency.code} -> " +
                    _uiState.value.toCurrency.code
        )
    }

    // Validates the request and performs the local prototype conversion.
// This is the main section that will later call the live currency API instead!!
    fun convertCurrency(
        request: CurrencyRequest
    ) {

        val fromCurrency =
            currencies.find {
                it.code == request.from
            }

        val toCurrency =
            currencies.find {
                it.code == request.to
            }

        // Protects the conversion even if invalid data reaches the ViewModel.
        if (
            !request.amount.isFinite() ||
            request.amount <= 0.0
        ) {

            _uiState.value =
                _uiState.value.copy(
                    result = null,
                    errorMessage =
                        "Enter an amount greater than 0."
                )

            Log.w(
                TAG,
                "Conversion rejected because the amount was invalid."
            )

            return
        }

        if (
            fromCurrency == null ||
            toCurrency == null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    result = null,
                    errorMessage =
                        "A valid source and destination currency are required."
                )

            Log.w(
                TAG,
                "Conversion rejected because a currency was not recognised."
            )

            return
        }

        try {

            val rate =
                CurrencyConverter.calculateRate(
                    fromRateToZar =
                        fromCurrency.rateToZar,
                    toRateToZar =
                        toCurrency.rateToZar
                )

            val convertedAmount =
                CurrencyConverter.convert(
                    amount =
                        request.amount,
                    fromRateToZar =
                        fromCurrency.rateToZar,
                    toRateToZar =
                        toCurrency.rateToZar
                )

            val response =
                CurrencyResponse(
                    from =
                        request.from,
                    to =
                        request.to,
                    amount =
                        request.amount,
                    rate =
                        rate,
                    convertedAmount =
                        convertedAmount
                )

            _uiState.value =
                _uiState.value.copy(
                    result = response,
                    errorMessage = null
                )

            Log.d(
                TAG,
                "Conversion completed successfully."
            )

        } catch (
            exception:
            IllegalArgumentException
        ) {

            _uiState.value =
                _uiState.value.copy(
                    result = null,
                    errorMessage =
                        exception.message
                            ?: "Currency conversion failed."
                )

            Log.e(
                TAG,
                "Currency conversion failed.",
                exception
            )
        }
    }

    fun clearResult() {

        if (
            _uiState.value.result != null
        ) {

            // Removes the old result once the user changes the input.
            _uiState.value =
                _uiState.value.copy(
                    result = null
                )
        }
    }

    fun clearError() {

        if (
            _uiState.value.errorMessage != null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    errorMessage = null
                )
        }
    }
}