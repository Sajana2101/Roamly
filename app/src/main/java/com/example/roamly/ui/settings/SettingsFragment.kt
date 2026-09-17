package com.example.roamly.ui.settings


import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.roamly.R


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    // current settings
    private var displayName = "Jane Williams"
    private var email = "jane@example.com"
    private var language = "English"

    private var notificationsEnabled = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // get views from xml
        val nameInput = view.findViewById<EditText>(R.id.etDisplayName)
        val emailInput = view.findViewById<EditText>(R.id.etEmail)
        val languageCard = view.findViewById<LinearLayout>(R.id.settingLanguage)
        val languageText = view.findViewById<TextView>(R.id.txtLanguage)
        val notificationSwitch = view.findViewById<Switch>(R.id.switchNotifications)
        val notificationStatus = view.findViewById<TextView>(R.id.txtNotificationStatus)
        val saveButton = view.findViewById<Button>(R.id.btnSaveSettings)
        val signOutButton = view.findViewById<Button>(R.id.btnSignOut)

        // display current settings
        nameInput.setText(displayName)
        emailInput.setText(email)
        languageText.setText(language)
        notificationSwitch.isChecked = notificationsEnabled

        updateNotificationText(notificationSwitch, notificationStatus)

        // language selection
        //create dropdown between 4 language options
        languageCard.setOnClickListener {
            val languages = arrayOf("English", "Afrikaans", "isiZulu", "isiXhosa")

            val selectedLanguage = languages.indexOf(language)

            AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, selectedLanguage)
                { dialog, which ->
                    language = languages[which]
                    languageText.text = language
                    dialog.dismiss()
                }.show()
        }

        // notifications
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationsEnabled = isChecked

            updateNotificationText(notificationSwitch, notificationStatus)
        }

        // save all settings
        saveButton.setOnClickListener {
            val newName = nameInput.text.toString().trim()
            val newEmail = emailInput.text.toString().trim()

            if (newName.isEmpty()) {
                nameInput.error = "Please enter your name"
                return@setOnClickListener
            }
            if (newEmail.isEmpty()) {
                emailInput.error = "Please enter your email"
                return@setOnClickListener
            }

            // Save profile information
            displayName = newName
            email = newEmail

            //Save notification setting
            notificationsEnabled = notificationSwitch.isChecked
            showSaveConfirmation()
        }

            //SIGN OUT
            signOutButton.setOnClickListener { showSignOutConfirmation() }


    }


        // Update notification message
        private fun updateNotificationText(notificationSwitch: Switch, notificationStatus: TextView ) {

            if (notificationSwitch.isChecked) {
                notificationStatus.text = "Receive travel reminders"
            }
            else {
                notificationStatus.text = "Travel reminders are turned off"
            }
        }
        // Save confirmation dialog
        private fun showSaveConfirmation() {
            val dialog = AlertDialog.
            Builder(requireContext())
                .setTitle("Settings Saved")
                .setMessage("Your settings have been updated successfully.")
                .setPositiveButton("OK", null)
                .create()

            dialog.show()
            dialog.window?.setBackgroundDrawableResource( R.drawable.dialog_background )

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            styleDialogButton( okButton, R.drawable.dialog_button_blue )
        }

        // Sign out confirmation
        private fun showSignOutConfirmation() {
            val dialog = AlertDialog
                .Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of Roamly?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign Out", null)
                .create()

            dialog.show()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

            val signOutButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            styleDialogButton(signOutButton, R.drawable.settings_button_red)

            signOutButton.setOnClickListener {
                // Temporary sign out
                // Real logout will be connected later
                dialog.dismiss()
            }
        }

            //Style dialog buttons
            private fun styleDialogButton( button: Button, background: Int ) {
                button.backgroundTintList = null
                button.setBackgroundResource(background)
                button.setTextColor(Color.WHITE)
                button.setPadding(32, 12, 32, 12)
                button.alpha = 1f
            }


}