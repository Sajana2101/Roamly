package com.example.roamly.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsLanguageMapperTest {

    @Test
    fun supportedLanguageNames_areConvertedToCorrectCodes() {

        assertEquals(
            "en",
            SettingsLanguageMapper
                .toLanguageCode(
                    "English"
                )
        )

        assertEquals(
            "af",
            SettingsLanguageMapper
                .toLanguageCode(
                    "Afrikaans"
                )
        )

        assertEquals(
            "zu",
            SettingsLanguageMapper
                .toLanguageCode(
                    "isiZulu"
                )
        )

        assertEquals(
            "xh",
            SettingsLanguageMapper
                .toLanguageCode(
                    "isiXhosa"
                )
        )
    }

    @Test
    fun supportedLanguageCodes_areConvertedToDisplayNames() {

        assertEquals(
            "English",
            SettingsLanguageMapper
                .toDisplayName(
                    "en"
                )
        )

        assertEquals(
            "Afrikaans",
            SettingsLanguageMapper
                .toDisplayName(
                    "af"
                )
        )

        assertEquals(
            "isiZulu",
            SettingsLanguageMapper
                .toDisplayName(
                    "zu"
                )
        )

        assertEquals(
            "isiXhosa",
            SettingsLanguageMapper
                .toDisplayName(
                    "xh"
                )
        )
    }

    @Test
    fun regionalLanguageCodes_areHandledCorrectly() {

        assertEquals(
            "Afrikaans",
            SettingsLanguageMapper
                .toDisplayName(
                    "af-ZA"
                )
        )

        assertEquals(
            "isiZulu",
            SettingsLanguageMapper
                .toDisplayName(
                    "zu-ZA"
                )
        )

        assertEquals(
            "isiXhosa",
            SettingsLanguageMapper
                .toDisplayName(
                    "xh-ZA"
                )
        )
    }

    @Test
    fun unknownLanguages_fallBackToEnglish() {

        assertEquals(
            "en",
            SettingsLanguageMapper
                .toLanguageCode(
                    "Unknown"
                )
        )

        assertEquals(
            "English",
            SettingsLanguageMapper
                .toDisplayName(
                    "unknown"
                )
        )
    }
}