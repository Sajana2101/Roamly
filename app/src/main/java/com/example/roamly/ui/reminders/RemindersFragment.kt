
package com.example.roamly.ui.reminders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.Reminder
import com.example.roamly.data.reminders.ReminderAlarmScheduler
import com.example.roamly.data.reminders.ReminderLocalStore
import com.example.roamly.data.repository.HolidayRepository
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
    private lateinit var holidayRepository: HolidayRepository

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        reminderStore = ReminderLocalStore(requireContext())
        holidayRepository = HolidayRepository(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(
            R.id.remindersRecyclerView
        )

        reminderAdapter = ReminderAdapter(
            onEdit = { showReminderDialog(it) },
            onDelete = { confirmDeleteReminder(it) },
            onEnabledChange = { reminder, enabled ->
                updateReminderEnabled(reminder, enabled)
            },
            holidayNameForId = { holidayId ->
                holidayId?.let {
                    holidayRepository.getHolidayById(it)?.name
                }
            }
        )

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerView.adapter = reminderAdapter

        view.findViewById<MaterialButton>(
            R.id.btnAddReminder
        ).setOnClickListener {
            showReminderDialog()
        }

        reminderStore.getReminders().forEach {
            ReminderAlarmScheduler.schedule(requireContext(), it)
        }

        refreshReminderList()
    }

    private fun refreshReminderList() {
        val currentView = view ?: return
        val reminders = reminderStore.getReminders()

        val recyclerView = currentView.findViewById<RecyclerView>(
            R.id.remindersRecyclerView
        )

        val emptyState = currentView.findViewById<View>(
            R.id.remindersEmptyState
        )

        reminderAdapter.updateReminders(reminders)

        recyclerView.visibility =
            if (reminders.isEmpty()) View.GONE else View.VISIBLE

        emptyState.visibility =
            if (reminders.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showReminderDialog(
        existingReminder: Reminder? = null
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder, null)

        val dialogTitle = dialogView.findViewById<TextView>(
            R.id.reminderDialogTitle
        )

        val titleInputLayout =
            dialogView.findViewById<TextInputLayout>(
                R.id.reminderTitleInputLayout
            )

        val titleInput =
            dialogView.findViewById<TextInputEditText>(
                R.id.reminderTitleEditText
            )

        val descriptionInput =
            dialogView.findViewById<TextInputEditText>(
                R.id.reminderDescriptionEditText
            )

        val holidayButton = dialogView.findViewById<MaterialButton>(
            R.id.reminderHolidayButton
        )

        val dateButton = dialogView.findViewById<MaterialButton>(
            R.id.reminderDateButton
        )

        val timeButton = dialogView.findViewById<MaterialButton>(
            R.id.reminderTimeButton
        )

        val enabledSwitch =
            dialogView.findViewById<SwitchMaterial>(
                R.id.reminderEnabledSwitch
            )

        val holidays = holidayRepository.getHolidays()
            .sortedBy { it.name.lowercase(Locale.getDefault()) }

        var selectedHolidayId = existingReminder?.holidayId
        var selectedDate: Calendar? = null
        var selectedTime: Calendar? = null

        val dateFormat = SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

        val timeFormat = DateFormat.getTimeFormat(
            requireContext()
        )

        if (existingReminder != null) {
            dialogTitle.setText(R.string.reminders_edit)
            titleInput.setText(existingReminder.title)
            descriptionInput.setText(existingReminder.description)
            enabledSwitch.isChecked = existingReminder.isEnabled

            val calendar = Calendar.getInstance().apply {
                timeInMillis = existingReminder.scheduledAtMillis
            }

            selectedDate = calendar.clone() as Calendar
            selectedTime = calendar.clone() as Calendar

            dateButton.text = dateFormat.format(calendar.time)
            timeButton.text = timeFormat.format(calendar.time)
        } else {
            dialogTitle.setText(R.string.reminders_add)
        }

        holidays.firstOrNull {
            it.holidayId == selectedHolidayId
        }?.let {
            holidayButton.text = it.name
        }

        holidayButton.setOnClickListener {
            if (holidays.isEmpty()) {
                showMessage(getString(R.string.reminders_no_holidays))
                return@setOnClickListener
            }

            val holidayNames = holidays.map {
                "${it.name} (${it.location})"
            }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reminders_select_holiday)
                .setItems(holidayNames) { _, index ->
                    val holiday = holidays[index]
                    selectedHolidayId = holiday.holidayId
                    holidayButton.text = holiday.name
                }
                .show()
        }

        dateButton.setOnClickListener {
            val calendar = selectedDate ?: Calendar.getInstance()

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }

                    dateButton.text = dateFormat.format(
                        selectedDate!!.time
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

            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    selectedTime = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    timeButton.text = timeFormat.format(
                        selectedTime!!.time
                    )
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(requireContext())
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(
                if (existingReminder == null) {
                    R.string.reminders_save
                } else {
                    R.string.reminders_edit
                },
                null
            )
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

                val holidayId = selectedHolidayId

                if (
                    holidayId == null ||
                    holidays.none { it.holidayId == holidayId }
                ) {
                    showMessage(getString(R.string.reminders_holiday_required))
                    return@setOnClickListener
                }

                val date = selectedDate
                val time = selectedTime

                if (date == null || time == null) {
                    showMessage(getString(R.string.reminders_invalid_date))
                    return@setOnClickListener
                }

                val scheduledTime = Calendar.getInstance().apply {
                    set(
                        date.get(Calendar.YEAR),
                        date.get(Calendar.MONTH),
                        date.get(Calendar.DAY_OF_MONTH),
                        time.get(Calendar.HOUR_OF_DAY),
                        time.get(Calendar.MINUTE),
                        0
                    )
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                if (scheduledTime <= System.currentTimeMillis()) {
                    showMessage(getString(R.string.reminders_invalid_date))
                    return@setOnClickListener
                }

                val reminder = Reminder(
                    reminderId = existingReminder?.reminderId
                        ?: UUID.randomUUID().toString(),
                    title = title,
                    scheduledAtMillis = scheduledTime,
                    isEnabled = enabledSwitch.isChecked,
                    description = descriptionInput.text
                        ?.toString()
                        ?.trim()
                        .orEmpty(),
                    holidayId = holidayId
                )

                val currentReminders = reminderStore.getReminders()

                val updatedReminders = if (existingReminder == null) {
                    currentReminders + reminder
                } else {
                    currentReminders.map {
                        if (it.reminderId == existingReminder.reminderId) {
                            reminder
                        } else {
                            it
                        }
                    }
                }

                reminderStore.saveReminders(updatedReminders)

                ReminderAlarmScheduler.schedule(
                    requireContext(),
                    reminder
                )

                refreshReminderList()

                showMessage(
                    if (existingReminder == null) {
                        "Reminder saved successfully"
                    } else {
                        "Reminder updated successfully"
                    }
                )

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDeleteReminder(reminder: Reminder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reminders_delete)
            .setMessage(R.string.reminders_delete_confirmation)
            .setNegativeButton(R.string.reminders_cancel, null)
            .setPositiveButton(R.string.reminders_delete) { _, _ ->
                val updatedReminders = reminderStore
                    .getReminders()
                    .filterNot { it.reminderId == reminder.reminderId }

                reminderStore.saveReminders(updatedReminders)

                ReminderAlarmScheduler.cancel(
                    requireContext(),
                    reminder.reminderId
                )

                refreshReminderList()
                showMessage("Reminder deleted successfully")
            }
            .show()
    }

    private fun updateReminderEnabled(
        reminder: Reminder,
        enabled: Boolean
    ) {
        if (
            enabled &&
            reminder.scheduledAtMillis <= System.currentTimeMillis()
        ) {
            showMessage("Cannot enable a reminder in the past")
            refreshReminderList()
            return
        }

        val updatedReminder = reminder.copy(isEnabled = enabled)

        val updatedReminders = reminderStore.getReminders().map {
            if (it.reminderId == reminder.reminderId) {
                updatedReminder
            } else {
                it
            }
        }

        reminderStore.saveReminders(updatedReminders)

        ReminderAlarmScheduler.schedule(
            requireContext(),
            updatedReminder
        )

        refreshReminderList()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
