package com.example.roamly.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.auth.SessionManager
import com.google.firebase.auth.FirebaseAuth
//imports for part 3
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.roamly.data.settings.SettingsPreferencesRepository
import com.example.roamly.data.settings.settingsDataStore
import kotlinx.coroutines.launch

class SettingsFragment :
    Fragment(
        R.layout.fragment_settings
    ) {

    private lateinit var sessionManager:
            SessionManager

    private lateinit var firebaseAuth:
            FirebaseAuth

    private val settingsViewModel:
            SettingsViewModel by viewModels {

        SettingsViewModel.Factory(
            SettingsPreferencesRepository(
                requireContext()
                    .settingsDataStore
            )
        )
    }

    private var displayName =
        ""

    private var email =
        ""

    private var language =
        "English"

    private var homeCurrency =
        "ZAR"

    private var notificationsEnabled =
        true

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        sessionManager =
            SessionManager(
                requireContext()
            )

        firebaseAuth =
            FirebaseAuth.getInstance()

        loadAuthenticatedUser()

        val nameInput =
            view.findViewById<EditText>(
                R.id.etDisplayName
            )

        val emailInput =
            view.findViewById<EditText>(
                R.id.etEmail
            )

        val languageCard =
            view.findViewById<LinearLayout>(
                R.id.settingLanguage
            )

        val languageText =
            view.findViewById<TextView>(
                R.id.txtLanguage
            )

        val currencyCard =
            view.findViewById<LinearLayout>(
                R.id.settingCurrency
            )

        val currencyText =
            view.findViewById<TextView>(
                R.id.txtCurrency
            )

        val notificationSwitch =
            view.findViewById<Switch>(
                R.id.switchNotifications
            )

        val notificationStatus =
            view.findViewById<TextView>(
                R.id.txtNotificationStatus
            )

        val saveButton =
            view.findViewById<Button>(
                R.id.btnSaveSettings
            )

        val signOutButton =
            view.findViewById<Button>(
                R.id.btnSignOut
            )

        nameInput.setText(
            displayName
        )

        emailInput.setText(
            email
        )

        emailInput.isEnabled =
            false

        emailInput.alpha =
            0.75f
        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        settingsViewModel
                            .settings
                            .collect { savedSettings ->

                                language =
                                    savedSettings.language

                                homeCurrency =
                                    savedSettings.homeCurrency

                                notificationsEnabled =
                                    savedSettings.notificationsEnabled

                                languageText.text =
                                    language

                                currencyText.text =
                                    getCurrencyDisplayName(
                                        homeCurrency
                                    )

                                notificationSwitch.isChecked =
                                    notificationsEnabled

                                updateNotificationText(
                                    notificationSwitch,
                                    notificationStatus
                                )
                            }
                    }
            }

        languageCard
            .setOnClickListener {

                val languages =
                    arrayOf(
                        "English",
                        "Afrikaans",
                        "isiZulu",
                        "isiXhosa"
                    )

                val selectedLanguage =
                    languages.indexOf(
                        language
                    )

                AlertDialog
                    .Builder(
                        requireContext()
                    )
                    .setTitle(
                        "Select Language"
                    )
                    .setSingleChoiceItems(
                        languages,
                        if (
                            selectedLanguage >= 0
                        ) {
                            selectedLanguage
                        } else {
                            0
                        }
                    ) { dialog,
                        which ->

                        language =
                            languages[which]

                        languageText.text =
                            language

                        dialog.dismiss()
                    }
                    .show()
            }

        currencyCard
            .setOnClickListener {

                val currencyCodes =
                    arrayOf(
                        "ZAR",
                        "USD",
                        "EUR",
                        "GBP",
                        "AED",
                        "JPY",
                        "AUD",
                        "CAD"
                    )

                val currencyNames =
                    currencyCodes.map { code ->

                        getCurrencyDisplayName(
                            code
                        )
                    }.toTypedArray()

                val selectedCurrency =
                    currencyCodes.indexOf(
                        homeCurrency
                    )

                AlertDialog
                    .Builder(
                        requireContext()
                    )
                    .setTitle(
                        "Select Home Currency"
                    )
                    .setSingleChoiceItems(
                        currencyNames,
                        if (
                            selectedCurrency >= 0
                        ) {
                            selectedCurrency
                        } else {
                            0
                        }
                    ) { dialog,
                        which ->

                        homeCurrency =
                            currencyCodes[which]

                        currencyText.text =
                            getCurrencyDisplayName(
                                homeCurrency
                            )

                        dialog.dismiss()
                    }
                    .show()
            }

        notificationSwitch
            .setOnCheckedChangeListener { _,
                                          isChecked ->

                notificationsEnabled =
                    isChecked

                updateNotificationText(
                    notificationSwitch,
                    notificationStatus
                )
            }

        saveButton
            .setOnClickListener {

                val newName =
                    nameInput.text
                        .toString()
                        .trim()

                if (
                    newName.isEmpty()
                ) {

                    nameInput.error =
                        "Please enter your name"

                    return@setOnClickListener
                }

                displayName =
                    newName

                notificationsEnabled =
                    notificationSwitch
                        .isChecked

                sessionManager
                    .updateName(
                        displayName
                    )

                settingsViewModel
                    .saveSettings(
                        language =
                            language,
                        homeCurrency =
                            homeCurrency,
                        notificationsEnabled =
                            notificationsEnabled
                    )

                showSaveConfirmation()
            }

        signOutButton
            .setOnClickListener {

                showSignOutConfirmation()
            }
    }

    private fun loadAuthenticatedUser() {

        displayName =
            sessionManager
                .getName()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: firebaseAuth
                    .currentUser
                    ?.displayName
                        ?: "Roamly User"

        email =
            sessionManager
                .getEmail()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: firebaseAuth
                    .currentUser
                    ?.email
                        ?: ""
    }

    private fun updateNotificationText(
        notificationSwitch: Switch,
        notificationStatus: TextView
    ) {

        if (
            notificationSwitch.isChecked
        ) {

            notificationStatus.text =
                "Receive travel reminders"

        } else {

            notificationStatus.text =
                "Travel reminders are turned off"
        }
    }

    private fun getCurrencyDisplayName(
        currencyCode: String
    ): String {

        return when (
            currencyCode
        ) {

            "USD" ->
                "USD - US Dollar"

            "EUR" ->
                "EUR - Euro"

            "GBP" ->
                "GBP - British Pound"

            "AED" ->
                "AED - UAE Dirham"

            "JPY" ->
                "JPY - Japanese Yen"

            "AUD" ->
                "AUD - Australian Dollar"

            "CAD" ->
                "CAD - Canadian Dollar"

            else ->
                "ZAR - South African Rand"
        }
    }

    private fun showSaveConfirmation() {

        val dialog =
            AlertDialog
                .Builder(
                    requireContext()
                )
                .setTitle(
                    "Settings Saved"
                )
                .setMessage(
                    "Your settings have been updated successfully."
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .create()

        dialog.show()

        dialog.window
            ?.setBackgroundDrawableResource(
                R.drawable.dialog_background
            )

        val okButton =
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

        styleDialogButton(
            okButton,
            R.drawable.dialog_button_blue
        )
    }

    private fun showSignOutConfirmation() {

        val dialog =
            AlertDialog
                .Builder(
                    requireContext()
                )
                .setTitle(
                    "Sign Out"
                )
                .setMessage(
                    "Are you sure you want to sign out of Roamly?"
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .setPositiveButton(
                    "Sign Out",
                    null
                )
                .create()

        dialog.show()

        dialog.window
            ?.setBackgroundDrawableResource(
                R.drawable.dialog_background
            )

        val signOutButton =
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

        styleDialogButton(
            signOutButton,
            R.drawable.settings_button_red
        )

        signOutButton
            .setOnClickListener {

                dialog.dismiss()

                performSignOut()
            }
    }

    private fun performSignOut() {

        firebaseAuth.signOut()

        sessionManager.clearSession()

        (
                requireActivity()
                        as MainActivity
                )
            .showLogin()
    }

    private fun styleDialogButton(
        button: Button,
        background: Int
    ) {

        button.backgroundTintList =
            null

        button.setBackgroundResource(
            background
        )

        button.setTextColor(
            Color.WHITE
        )

        button.setPadding(
            32,
            12,
            32,
            12
        )

        button.alpha =
            1f
    }
}
