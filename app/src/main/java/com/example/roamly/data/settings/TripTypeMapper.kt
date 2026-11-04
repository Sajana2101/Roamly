package com.example.roamly.data.settings

import android.content.Context
import com.example.roamly.R

object TripTypeMapper {

    fun toDisplayName(
        context: Context,
        tripType: String
    ): String {

        val tripTypes = context.resources
            .getStringArray(R.array.trip_types)

        val englishTypes = arrayOf(
            "Leisure",
            "Business",
            "Family",
            "Romantic",
            "Adventure",
            "Road Trip",
            "Solo",
            "Other"
        )

        val index = englishTypes.indexOf(tripType)

        return if (index != -1) {
            tripTypes[index]
        } else {
            tripType
        }
    }

    fun toStoredValue(
        context: Context,
        displayName: String
    ): String {

        val tripTypes = context.resources
            .getStringArray(R.array.trip_types)

        val englishTypes = arrayOf(
            "Leisure",
            "Business",
            "Family",
            "Romantic",
            "Adventure",
            "Road Trip",
            "Solo",
            "Other"
        )

        val index = tripTypes.indexOf(displayName)

        return if (index != -1) {
            englishTypes[index]
        } else {
            displayName
        }
    }
}