package com.example.roamly.ui.documents

import android.content.Context
import com.example.roamly.R

object DocumentTypeMapper {

    private val englishTypes = arrayOf(
        "Identification",
        "Visa",
        "Flight",
        "Accommodation",
        "Insurance",
        "Booking",
        "Other"
    )

    fun toDisplayName(context: Context, storedValue: String): String {
        val translatedTypes =
            context.resources.getStringArray(R.array.document_types)

        val index = englishTypes.indexOf(storedValue)

        return if (index != -1) translatedTypes[index] else storedValue
    }

    fun toStoredValue(context: Context, displayName: String): String {
        val translatedTypes =
            context.resources.getStringArray(R.array.document_types)

        val index = translatedTypes.indexOf(displayName)

        return if (index != -1) englishTypes[index] else displayName
    }
}