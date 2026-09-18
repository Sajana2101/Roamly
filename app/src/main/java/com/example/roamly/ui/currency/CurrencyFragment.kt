package com.example.roamly.ui.currency

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.roamly.R
import com.example.roamly.data.model.CurrencyOption
import com.example.roamly.data.model.CurrencyRequest
import com.example.roamly.data.model.CurrencyResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class CurrencyFragment :
    Fragment(R.layout.fragment_currency) {

    companion object {
        private const val TAG =
            "CurrencyFragment"
    }

    // Keeps Currency data and calculations outside the Fragment so the UI only handles display and input
    private val viewModel:
            CurrencyViewModel by viewModels()

    private lateinit var layoutAmount:
            TextInputLayout

    private lateinit var etAmount:
            TextInputEditText

    private lateinit var dropdownFrom:
            MaterialAutoCompleteTextView

    private lateinit var dropdownTo:
            MaterialAutoCompleteTextView

    private lateinit var cardResult:
            MaterialCardView

    private lateinit var tvConvertedValue:
            TextView

    private lateinit var tvConversionSummary:
            TextView

    private lateinit var tvExchangeRate:
            TextView

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        Log.d(
            TAG,
            "Currency screen opened."
        )

        // Connects the Currency screen to the views in fragment_currency.xml.
        layoutAmount =
            view.findViewById(
                R.id.layoutCurrencyAmount
            )

        etAmount =
            view.findViewById(
                R.id.etCurrencyAmount
            )

        dropdownFrom =
            view.findViewById(
                R.id.dropdownFromCurrency
            )

        dropdownTo =
            view.findViewById(
                R.id.dropdownToCurrency
            )

        cardResult =
            view.findViewById(
                R.id.cardCurrencyResult
            )

        tvConvertedValue =
            view.findViewById(
                R.id.tvConvertedValue
            )

        tvConversionSummary =
            view.findViewById(
                R.id.tvConversionSummary
            )

        tvExchangeRate =
            view.findViewById(
                R.id.tvExchangeRate
            )

        val btnSwap =
            view.findViewById<MaterialButton>(
                R.id.btnSwapCurrencies
            )

        val btnConvert =
            view.findViewById<MaterialButton>(
                R.id.btnConvertCurrency
            )

        setupCurrencyDropdowns()

        // Changing the entered amount removes the previous conversion result.
        etAmount.doAfterTextChanged {

            layoutAmount.error = null

            viewModel.clearError()
            viewModel.clearResult()
        }

        btnSwap.setOnClickListener {

            viewModel.swapCurrencies()
        }

        btnConvert.setOnClickListener {

            convertEnteredAmount()
        }

        observeCurrencyState()
    }

    // Prepares both dropdown menus and sends each user selection to the ViewModel.
    private fun setupCurrencyDropdowns() {

        // Both dropdowns use the same list of supported currencies.
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
                viewModel.currencies
            )

        dropdownFrom.setAdapter(
            adapter
        )

        dropdownTo.setAdapter(
            adapter
        )

        // Shows the dropdown when the field is tapped.
        dropdownFrom.setOnClickListener {

            dropdownFrom.showDropDown()
        }

        dropdownTo.setOnClickListener {

            dropdownTo.showDropDown()
        }

        dropdownFrom.setOnItemClickListener {
                _, _, position, _ ->

            val selectedCurrency =
                viewModel
                    .currencies[position]

            viewModel.selectFromCurrency(
                selectedCurrency
            )
        }

        dropdownTo.setOnItemClickListener {
                _, _, position, _ ->

            val selectedCurrency =
                viewModel
                    .currencies[position]

            viewModel.selectToCurrency(
                selectedCurrency
            )
        }
    }

    // Reads and validates the entered amount before creating the conversion request
    private fun convertEnteredAmount() {

        layoutAmount.error = null

        val amountText =
            etAmount.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val amount =
            amountText.toDoubleOrNull()

        // Stops an invalid amount before the conversion reaches the ViewModel.
        if (
            amount == null ||
            !amount.isFinite() ||
            amount <= 0.0
        ) {

            layoutAmount.error =
                getString(
                    R.string.currency_invalid_amount
                )

            cardResult.visibility =
                View.GONE

            Log.w(
                TAG,
                "Invalid currency amount entered."
            )

            return
        }

        val currentState =
            viewModel.uiState.value

        val request =
            CurrencyRequest(
                from =
                    currentState
                        .fromCurrency.code,
                to =
                    currentState
                        .toCurrency.code,
                amount =
                    amount
            )

        viewModel.convertCurrency(
            request
        )
    }

    // Watches the ViewModel state and refreshes the screen whenever the currencies or result change
    private fun observeCurrencyState() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        viewModel.uiState.collect {
                                state ->

                            // Keeps both dropdowns synchronised with the ViewModel.
                            dropdownFrom.setText(
                                state.fromCurrency
                                    .toString(),
                                false
                            )

                            dropdownTo.setText(
                                state.toCurrency
                                    .toString(),
                                false
                            )

                            val result =
                                state.result

                            if (
                                result == null
                            ) {

                                cardResult.visibility =
                                    View.GONE

                            } else {

                                renderConversionResult(
                                    result =
                                        result,
                                    fromCurrency =
                                        state.fromCurrency,
                                    toCurrency =
                                        state.toCurrency
                                )
                            }

                            if (
                                state.errorMessage != null
                            ) {

                                layoutAmount.error =
                                    state.errorMessage
                            }
                        }
                    }
            }
    }

    // Formats the completed conversion into the result card shown to the user
    private fun renderConversionResult(
        result: CurrencyResponse,
        fromCurrency: CurrencyOption,
        toCurrency: CurrencyOption
    ) {

        val amountFormat =
            DecimalFormat(
                "#,##0.00"
            )

        val rateFormat =
            DecimalFormat(
                "0.0000"
            )

        // Displays the converted value as the main result.
        tvConvertedValue.text =
            "${toCurrency.symbol}" +
                    amountFormat.format(
                        result.convertedAmount
                    )

        // Shows the full conversion calculation below the main result.
        tvConversionSummary.text =
            "${fromCurrency.symbol}" +
                    amountFormat.format(
                        result.amount
                    ) +
                    " ${fromCurrency.code} = " +
                    "${toCurrency.symbol}" +
                    amountFormat.format(
                        result.convertedAmount
                    ) +
                    " ${toCurrency.code}"

        // Shows the direct exchange rate separately.
        tvExchangeRate.text =
            "1 ${fromCurrency.code} = " +
                    "${rateFormat.format(result.rate)} " +
                    toCurrency.code

        cardResult.visibility =
            View.VISIBLE

        Log.d(
            TAG,
            "Currency result displayed."
        )
    }
}