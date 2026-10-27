
package com.example.roamly.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.example.roamly.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RemindersFragment : Fragment(R.layout.fragment_reminders) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val addReminderButton =
            view.findViewById<MaterialButton>(
                R.id.btnAddReminder
            )

        addReminderButton.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder, null)

        val dateButton =
            dialogView.findViewById<MaterialButton>(
                R.id.reminderDateButton
            )

        val timeButton =
            dialogView.findViewById<MaterialButton>(
                R.id.reminderTimeButton
            )

        var selectedDate: Calendar? = null
        var selectedTime: Calendar? = null

        dateButton.setOnClickListener {
            val calendar = selectedDate ?: Calendar.getInstance()

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val chosenDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }

                    selectedDate = chosenDate

                    val dateFormat = SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                    )

                    dateButton.text = dateFormat.format(
                        chosenDate.time
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            datePicker.datePicker.minDate = today.timeInMillis
            datePicker.show()
        }

        timeButton.setOnClickListener {
            val calendar = selectedTime ?: Calendar.getInstance()

            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val chosenTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    selectedTime = chosenTime

                    val timeFormat = DateFormat.getTimeFormat(
                        requireContext()
                    )

                    timeButton.text = timeFormat.format(
                        chosenTime.time
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(requireContext())
            )

            timePicker.show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton(R.string.reminders_cancel, null)
            .show()
    }
}
