package com.example.roamly

import com.example.roamly.data.util.CurrencyConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun convertZarToEuro_returnsExpectedValue() {

        // This just tests the same ZAR to EUR example used on the Currency screen.
        val result =
            CurrencyConverter.convert(
                amount = 1000.0,
                fromRateToZar = 1.0,
                toRateToZar = 20.0
            )

        assertEquals(
            50.0,
            result,
            0.001
        )
    }

    @Test
    fun convertEuroToZar_returnsExpectedValue() {

        // Confirms that the same calculation also works correctly in reverse.
        val result =
            CurrencyConverter.convert(
                amount = 50.0,
                fromRateToZar = 20.0,
                toRateToZar = 1.0
            )

        assertEquals(
            1000.0,
            result,
            0.001
        )
    }

    @Test
    fun convertingSameCurrency_returnsSameAmount() {

        // Converting between the same currency should not change the amount
        val result =
            CurrencyConverter.convert(
                amount = 250.0,
                fromRateToZar = 17.5,
                toRateToZar = 17.5
            )

        assertEquals(
            250.0,
            result,
            0.001
        )
    }

    @Test
    fun calculateRate_returnsExpectedRate() {

        // Checks that the direct exchange rate is calculated correctly!
        val rate =
            CurrencyConverter.calculateRate(
                fromRateToZar = 1.0,
                toRateToZar = 20.0
            )

        assertEquals(
            0.05,
            rate,
            0.0001
        )
    }

    @Test
    fun zeroAmount_throwsException() {

        // Invalid zero values should be rejected instead of producing a misleading result,
        assertThrows(
            IllegalArgumentException::class.java
        ) {

            CurrencyConverter.convert(
                amount = 0.0,
                fromRateToZar = 1.0,
                toRateToZar = 20.0
            )
        }
    }

    @Test
    fun negativeAmount_throwsException() {

        // Negative money values are not a valid input for this converter
        assertThrows(
            IllegalArgumentException::class.java
        ) {

            CurrencyConverter.convert(
                amount = -100.0,
                fromRateToZar = 1.0,
                toRateToZar = 20.0
            )
        }
    }

    @Test
    fun zeroDestinationRate_throwsException() {

        // A zero exchange rate must be rejected to prevent division by zero
        assertThrows(
            IllegalArgumentException::class.java
        ) {

            CurrencyConverter.convert(
                amount = 100.0,
                fromRateToZar = 1.0,
                toRateToZar = 0.0
            )
        }
    }
}