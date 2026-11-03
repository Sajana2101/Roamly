package com.example.roamly.data.settings

// converts the Settings screen language names into standard language codes

object SettingsLanguageMapper {

    fun toLanguageCode(
        language: String
    ): String {

        return when (
            language
        ) {

            "Afrikaans" ->
                "af"

            "isiZulu" ->
                "zu"

            "isiXhosa" ->
                "xh"

            else ->
                "en"
        }
    }

    fun toDisplayName(
        languageCode: String
    ): String {

        return when (
            languageCode
                .substringBefore("-")
                .lowercase()
        ) {

            "af" ->
                "Afrikaans"

            "zu" ->
                "isiZulu"

            "xh" ->
                "isiXhosa"

            else ->
                "English"
        }
    }
}