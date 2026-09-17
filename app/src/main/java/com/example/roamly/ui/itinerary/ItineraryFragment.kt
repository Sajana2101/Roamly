package com.example.roamly.ui.itinerary

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.roamly.R
import com.example.roamly.data.model.ItineraryDay
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ItineraryFragment : Fragment(R.layout.fragment_itinerary) {

    private lateinit var daysContainer: LinearLayout
    private lateinit var tvSelectedDayDate: TextView

    private val itineraryDays = mutableListOf(
        ItineraryDay(1, "2026/09/12"),
        ItineraryDay(2, "2026/09/13"),
        ItineraryDay(3, "2026/09/14"),
        ItineraryDay(4, "2026/09/15")
    )

    private var selectedDayNumber = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        daysContainer = view.findViewById(R.id.daysContainer)
        tvSelectedDayDate = view.findViewById(R.id.tvSelectedDayDate)

        val btnAddDay = view.findViewById<MaterialButton>(R.id.btnAddDay)

        btnAddDay.setOnClickListener {
            showAddDayDialog()
        }

        val btnEditDay = view.findViewById<MaterialButton>(R.id.btnEditDay)

        btnEditDay.setOnClickListener {
            showEditDayDialog()
        }

        val btnDeleteDay =
            view.findViewById<MaterialButton>(R.id.btnDeleteDay)

// Opens a confirmation dialog before removing the selected day
        btnDeleteDay.setOnClickListener {
            showDeleteDayDialog()
        }

        renderDayCards()
    }

    private fun renderDayCards() {

        daysContainer.removeAllViews()

        itineraryDays
            .sortedBy { it.dayNumber }
            .forEach { day ->

                val dayView = LayoutInflater.from(requireContext())
                    .inflate(
                        R.layout.item_itinerary_day,
                        daysContainer,
                        false
                    )

                val card = dayView as MaterialCardView

                val tvDayNumber =
                    dayView.findViewById<TextView>(R.id.tvDayNumber)

                val tvDayDate =
                    dayView.findViewById<TextView>(R.id.tvDayCardDate)

                val tvDayWeekday =
                    dayView.findViewById<TextView>(R.id.tvDayWeekday)

                tvDayNumber.text = "Day ${day.dayNumber}"

                val parsedDate = parseDate(day.date)

                if (parsedDate != null) {

                    tvDayDate.text =
                        SimpleDateFormat(
                            "dd MMM",
                            Locale.getDefault()
                        ).format(parsedDate)

                    tvDayWeekday.text =
                        SimpleDateFormat(
                            "EEE",
                            Locale.getDefault()
                        ).format(parsedDate)
                } else {
                    tvDayDate.text = day.date
                    tvDayWeekday.text = ""
                }

                applyDayCardStyle(
                    card,
                    tvDayNumber,
                    tvDayDate,
                    tvDayWeekday,
                    day.dayNumber == selectedDayNumber
                )

                card.setOnClickListener {

                    selectedDayNumber = day.dayNumber

                    updateSelectedDayHeading(day)

                    renderDayCards()
                }

                daysContainer.addView(dayView)
            }

        itineraryDays
            .find { it.dayNumber == selectedDayNumber }
            ?.let {
                updateSelectedDayHeading(it)
            }

        // Shows an empty state when all itinerary days have been deleted
        if (itineraryDays.isEmpty()) {
            tvSelectedDayDate.text = "No day selected"
        }
    }

    private fun applyDayCardStyle(
        card: MaterialCardView,
        tvDayNumber: TextView,
        tvDate: TextView,
        tvWeekday: TextView,
        isSelected: Boolean
    ) {

        if (isSelected) {

            card.setCardBackgroundColor(
                Color.parseColor("#2F8FD8")
            )

            card.strokeWidth = 0

            tvDayNumber.setTextColor(Color.WHITE)
            tvDate.setTextColor(Color.WHITE)

            tvWeekday.setTextColor(
                Color.parseColor("#E4F2FC")
            )

        } else {

            card.setCardBackgroundColor(Color.WHITE)

            card.strokeColor =
                Color.parseColor("#E1E7EC")

            card.strokeWidth =
                dpToPx(1)

            tvDayNumber.setTextColor(
                Color.parseColor("#7B8794")
            )

            tvDate.setTextColor(
                Color.parseColor("#172B3A")
            )

            tvWeekday.setTextColor(
                Color.parseColor("#7B8794")
            )
        }
    }

    private fun updateSelectedDayHeading(day: ItineraryDay) {

        val parsedDate = parseDate(day.date)

        tvSelectedDayDate.text =
            if (parsedDate != null) {

                SimpleDateFormat(
                    "EEEE, dd MMMM",
                    Locale.getDefault()
                ).format(parsedDate)

            } else {
                day.date
            }
    }

    private fun showAddDayDialog() {

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_day, null)

        val layoutDayNumber =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutDayNumber
            )

        val layoutDate =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutDate
            )

        val etDayNumber =
            dialogView.findViewById<TextInputEditText>(
                R.id.etDayNumber
            )

        val etDate =
            dialogView.findViewById<TextInputEditText>(
                R.id.etDate
            )

        val btnCancel =
            dialogView.findViewById<MaterialButton>(
                R.id.btnCancelAddDay
            )

        val btnAdd =
            dialogView.findViewById<MaterialButton>(
                R.id.btnConfirmAddDay
            )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etDate.setOnClickListener {

            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->

                    val selectedDate = String.format(
                        Locale.getDefault(),
                        "%04d/%02d/%02d",
                        year,
                        month + 1,
                        dayOfMonth
                    )

                    etDate.setText(selectedDate)
                    layoutDate.error = null
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnAdd.setOnClickListener {

            layoutDayNumber.error = null
            layoutDate.error = null

            val dayNumberText =
                etDayNumber.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val dateText =
                etDate.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val dayNumber =
                dayNumberText.toIntOrNull()

            var isValid = true

            if (dayNumber == null || dayNumber <= 0) {

                layoutDayNumber.error =
                    "Enter a valid day number."

                isValid = false

            } else if (
                itineraryDays.any {
                    it.dayNumber == dayNumber
                }
            ) {

                layoutDayNumber.error =
                    "That day number already exists."

                isValid = false
            }

            if (dateText.isEmpty()) {

                layoutDate.error =
                    "Please select a date."

                isValid = false

            } else if (!isDateWithinHoliday(dateText)) {

                layoutDate.error =
                    "Date must be within the selected holiday."

                isValid = false

            } else if (
                itineraryDays.any {
                    it.date == dateText
                }
            ) {

                layoutDate.error =
                    "That date already exists in the itinerary."

                isValid = false
            }

            if (isValid && dayNumber != null) {

                val newDay = ItineraryDay(
                    dayNumber = dayNumber,
                    date = dateText
                )

                itineraryDays.add(newDay)

                selectedDayNumber = newDay.dayNumber

                renderDayCards()

                Toast.makeText(
                    requireContext(),
                    "Day $dayNumber added.",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditDayDialog() {

        val selectedDay = itineraryDays.find {
            it.dayNumber == selectedDayNumber
        }

        if (selectedDay == null) {
            Toast.makeText(
                requireContext(),
                "Please select a day first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_day, null)

        val tvTitle =
            dialogView.findViewById<TextView>(
                R.id.tvDayDialogTitle
            )

        val tvSubtitle =
            dialogView.findViewById<TextView>(
                R.id.tvDayDialogSubtitle
            )

        val layoutDayNumber =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutDayNumber
            )

        val layoutDate =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutDate
            )

        val etDayNumber =
            dialogView.findViewById<TextInputEditText>(
                R.id.etDayNumber
            )

        val etDate =
            dialogView.findViewById<TextInputEditText>(
                R.id.etDate
            )

        val btnCancel =
            dialogView.findViewById<MaterialButton>(
                R.id.btnCancelAddDay
            )

        val btnSave =
            dialogView.findViewById<MaterialButton>(
                R.id.btnConfirmAddDay
            )

        // changed the Add Day dialog into Edit Day mode.
        tvTitle.text = "Edit Day"
        tvSubtitle.text = "Update the selected itinerary day."
        btnSave.text = "Save Changes"

        // Pre-fill the current values.
        etDayNumber.setText(
            selectedDay.dayNumber.toString()
        )

        etDate.setText(selectedDay.date)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etDate.setOnClickListener {

            val calendar = Calendar.getInstance()

            parseDate(selectedDay.date)?.let {
                calendar.time = it
            }

            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->

                    val selectedDate = String.format(
                        Locale.getDefault(),
                        "%04d/%02d/%02d",
                        year,
                        month + 1,
                        dayOfMonth
                    )

                    etDate.setText(selectedDate)
                    layoutDate.error = null
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {

            layoutDayNumber.error = null
            layoutDate.error = null

            val dayNumberText =
                etDayNumber.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val dateText =
                etDate.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val newDayNumber =
                dayNumberText.toIntOrNull()

            var isValid = true

            if (newDayNumber == null || newDayNumber <= 0) {

                layoutDayNumber.error =
                    "Enter a valid day number."

                isValid = false

            } else if (
                itineraryDays.any {
                    it.dayNumber == newDayNumber &&
                            it.dayNumber != selectedDay.dayNumber
                }
            ) {

                layoutDayNumber.error =
                    "That day number already exists."

                isValid = false
            }

            if (dateText.isEmpty()) {

                layoutDate.error =
                    "Please select a date."

                isValid = false

            } else if (!isDateWithinHoliday(dateText)) {

                layoutDate.error =
                    "Date must be within the selected holiday."

                isValid = false

            } else if (
                itineraryDays.any {
                    it.date == dateText &&
                            it.dayNumber != selectedDay.dayNumber
                }
            ) {

                layoutDate.error =
                    "That date already exists in the itinerary."

                isValid = false
            }

            if (isValid && newDayNumber != null) {

                val index =
                    itineraryDays.indexOfFirst {
                        it.dayNumber == selectedDay.dayNumber
                    }

                if (index != -1) {

                    itineraryDays[index] =
                        ItineraryDay(
                            dayNumber = newDayNumber,
                            date = dateText
                        )

                    selectedDayNumber = newDayNumber

                    renderDayCards()

                    Toast.makeText(
                        requireContext(),
                        "Day updated successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDayDialog() {

        // Finds the day that is currently selected on the itinerary
        val selectedDay = itineraryDays.find {
            it.dayNumber == selectedDayNumber
        }

        // Prevents the delete dialog from opening if no day is selected
        if (selectedDay == null) {
            Toast.makeText(
                requireContext(),
                "Please select a day first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_confirm_delete, null)

        val tvTitle =
            dialogView.findViewById<TextView>(
                R.id.tvDeleteTitle
            )

        val tvMessage =
            dialogView.findViewById<TextView>(
                R.id.tvDeleteMessage
            )

        val btnCancel =
            dialogView.findViewById<MaterialButton>(
                R.id.btnCancelDelete
            )

        val btnDelete =
            dialogView.findViewById<MaterialButton>(
                R.id.btnConfirmDelete
            )

        // Updates the reusable dialog with details of the selected day
        tvTitle.text = "Delete Day ${selectedDay.dayNumber}?"

        tvMessage.text =
            "This will delete Day ${selectedDay.dayNumber} " +
                    "and all activities added to this day."

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            // Removes the selected day from the local itinerary list
            itineraryDays.removeAll {
                it.dayNumber == selectedDay.dayNumber
            }

            if (itineraryDays.isNotEmpty()) {

                // Selects the first remaining day so the screen always has a valid selection
                selectedDayNumber =
                    itineraryDays
                        .minByOrNull { it.dayNumber }
                        ?.dayNumber ?: -1

            } else {

                // Uses -1 when the itinerary no longer contains any days
                selectedDayNumber = -1

                tvSelectedDayDate.text =
                    "No day selected"
            }

            // Redraws the day cards after the selected day has been removed
            renderDayCards()

            Toast.makeText(
                requireContext(),
                "Day ${selectedDay.dayNumber} deleted.",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isDateWithinHoliday(
        dateText: String
    ): Boolean {

        val selectedDate =
            parseDate(dateText) ?: return false

        val holidayStart =
            parseDate("2026/09/12") ?: return false

        val holidayEnd =
            parseDate("2026/09/20") ?: return false

        return !selectedDate.before(holidayStart) &&
                !selectedDate.after(holidayEnd)
    }

    private fun parseDate(dateText: String) =
        try {

            SimpleDateFormat(
                "yyyy/MM/dd",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }.parse(dateText)

        } catch (exception: Exception) {
            null
        }

    private fun dpToPx(dp: Int): Int {
        return (
                dp * resources.displayMetrics.density
                ).toInt()
    }
}