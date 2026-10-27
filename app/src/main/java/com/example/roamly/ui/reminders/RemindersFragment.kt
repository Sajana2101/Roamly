
package com.example.roamly.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.Reminder
import com.example.roamly.data.reminders.ReminderLocalStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class RemindersFragment : Fragment(R.layout.fragment_reminders) {

    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var reminderStore: ReminderLocalStore

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        reminderStore = ReminderLocalStore(requireContext())

        val addReminderButton =
            view.findViewById<MaterialButton>(
                R.id.btnAddReminder
            )

        val recyclerView =
            view.findViewById<RecyclerView>(
                R.id.remindersRecyclerView
            )

        reminderAdapter = ReminderAdapter(
            onEdit = {
                Toast.makeText(
                    requireContext(),
                    "Reminder editing is not available yet",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDelete = {
                Toast.makeText(
                    requireContext(),
                    "Reminder deletion is not available yet",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEnabledChange = { _, _ ->
                Toast.makeText(
                    requireContext(),
                    "Reminder updates are not available yet",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerView.adapter = reminderAdapter

        updateReminderList(view, reminderStore.getReminders())

        addReminderButton.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun updateReminderList(
        view: View,
        reminders: List<Reminder>
    ) {
        val recyclerView =
            view.findViewById<RecyclerView>(
                R.id.remindersRecyclerView
            )

        val emptyState =
            view.findViewById<View>(
                R.id.remindersEmptyState
            )

        reminderAdapter.updateReminders(reminders)

        if (reminders.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder, null)

        val titleInputLayout =
            dialogView.findViewById<TextInputLayout>(
                R.id.reminderTitleInputLayout
            )

        val titleInput =
            dialogView.findViewById<TextInputEditText>(
                R.id.reminderTitleEditText
            )

        val dateButton =
            dialogView.findViewById<MaterialButton>(
                R.id.reminderDateButton
            )

        val timeButton =
            dialogView.findViewById<MaterialButton>(
                R.id.reminderTimeButton
            )

        val enabledSwitch =
            dialogView.findViewById<SwitchMaterial>(
                R.id.reminderEnabledSwitch
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

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.reminders_save, null)
            .setNegativeButton(R.string.reminders_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                DialogInterface.BUTTON_POSITIVE
            ).setOnClickListener {

                val title = titleInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                if (title.isBlank()) {
                    titleInputLayout.error =
                        getString(R.string.reminders_invalid_title)

                    return@setOnClickListener
                }

                titleInputLayout.error = null

                val date = selectedDate
                val time = selectedTime

                if (date == null || time == null) {
                    Toast.makeText(
                        requireContext(),
                        R.string.reminders_invalid_date,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val scheduledDateTime = Calendar.getInstance().apply {
                    set(Calendar.YEAR, date.get(Calendar.YEAR))
                    set(Calendar.MONTH, date.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, time.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (
                    scheduledDateTime.timeInMillis <=
                    System.currentTimeMillis()
                ) {
                    Toast.makeText(
                        requireContext(),
                        R.string.reminders_invalid_date,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val reminder = Reminder(
                    reminderId = UUID.randomUUID().toString(),
                    title = title,
                    scheduledAtMillis = scheduledDateTime.timeInMillis,
                    isEnabled = enabledSwitch.isChecked
                )

                val currentReminders =
                    reminderStore.getReminders()

                reminderStore.saveReminders(
                    currentReminders + reminder
                )

                updateReminderList(
                    requireView(),
                    reminderStore.getReminders()
                )

                Toast.makeText(
                    requireContext(),
                    "Reminder saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
