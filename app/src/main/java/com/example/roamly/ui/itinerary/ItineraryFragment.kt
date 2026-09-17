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
import android.app.TimePickerDialog
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.Holiday

class ItineraryFragment : Fragment(R.layout.fragment_itinerary) {

    private lateinit var daysContainer: LinearLayout
    private lateinit var tvSelectedDayDate: TextView

    private lateinit var activitiesContainer: LinearLayout

    // Displays the name and date range of the holiday currently selected
    private lateinit var tvSelectedTripName: TextView
    private lateinit var tvSelectedTripDates: TextView

    // References the three holiday cards so the selected card can be highlighted
    private lateinit var cardHoneymoon: MaterialCardView
    private lateinit var cardBusinessTrip: MaterialCardView
    private lateinit var cardFamilyVacation: MaterialCardView

    // Stores the holidays available on the itinerary screen.
// These values are temporary local data until Holidays are loaded from the API.
    private val holidays = listOf(
        Holiday(
            holidayId = 1,
            name = "Honeymoon",
            location = "Greece",
            startDate = "2026/09/12",
            endDate = "2026/09/20"
        ),
        Holiday(
            holidayId = 2,
            name = "Business Trip",
            location = "Cape Town",
            startDate = "2026/10/03",
            endDate = "2026/10/08"
        ),
        Holiday(
            holidayId = 3,
            name = "Family Vacation",
            location = "Durban",
            startDate = "2026/12/15",
            endDate = "2026/12/22"
        )
    )

    // Honeymoon is selected when the itinerary screen first opens
    private var selectedHolidayId = 1

    // Keeps each holiday's itinerary days separate from the other holidays
    private val itineraryDaysByHoliday =
        mutableMapOf<Int, MutableList<ItineraryDay>>(
            1 to mutableListOf(
                ItineraryDay(1, "2026/09/12"),
                ItineraryDay(2, "2026/09/13"),
                ItineraryDay(3, "2026/09/14"),
                ItineraryDay(4, "2026/09/15")
            ),
            2 to mutableListOf(),
            3 to mutableListOf()
        )

    // Keeps each holiday's activities separate so they only appear under the correct trip
    private val activitiesByHoliday =
        mutableMapOf<Int, MutableList<ItineraryActivity>>(
            1 to mutableListOf(
                ItineraryActivity(
                    activityId = 1,
                    dayNumber = 1,
                    title = "Flight to Athens",
                    startTime = "08:30"
                ),
                ItineraryActivity(
                    activityId = 2,
                    dayNumber = 1,
                    title = "Hotel Check-in",
                    startTime = "19:30"
                )
            ),
            2 to mutableListOf(),
            3 to mutableListOf()
        )

    // These lists are changed whenever another holiday is selected
    private var itineraryDays =
        itineraryDaysByHoliday[selectedHolidayId]!!

    private var itineraryActivities =
        activitiesByHoliday[selectedHolidayId]!!

    private var selectedDayNumber = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        daysContainer = view.findViewById(R.id.daysContainer)
        tvSelectedDayDate = view.findViewById(R.id.tvSelectedDayDate)
        activitiesContainer =
            view.findViewById(R.id.activitiesContainer)

        // Connects the selected-trip information shown beneath the holiday cards
        tvSelectedTripName =
            view.findViewById(R.id.tvSelectedTripName)

        tvSelectedTripDates =
            view.findViewById(R.id.tvSelectedTripDates)

// Connects each holiday card so the user can switch between trips
        cardHoneymoon =
            view.findViewById(R.id.cardHoneymoon)

        cardBusinessTrip =
            view.findViewById(R.id.cardBusinessTrip)

        cardFamilyVacation =
            view.findViewById(R.id.cardFamilyVacation)

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

        val btnAddActivity =
            view.findViewById<MaterialButton>(R.id.btnAddActivity)

// Opens the activity form for the currently selected itinerary day
        btnAddActivity.setOnClickListener {
            showAddActivityDialog()
        }

        // Loads the Honeymoon itinerary when its card is selected
        cardHoneymoon.setOnClickListener {
            selectHoliday(1)
        }

// Loads the Business Trip itinerary when its card is selected
        cardBusinessTrip.setOnClickListener {
            selectHoliday(2)
        }

// Loads the Family Vacation itinerary when its card is selected
        cardFamilyVacation.setOnClickListener {
            selectHoliday(3)
        }

        val btnDeleteItinerary =
            view.findViewById<MaterialButton>(R.id.btnDeleteItinerary)

// Opens confirmation before clearing the selected holiday's itinerary
        btnDeleteItinerary.setOnClickListener {
            showDeleteItineraryDialog()
        }

        // Loads the default holiday and all itinerary information linked to it
        selectHoliday(selectedHolidayId)
    }

    private fun selectHoliday(holidayId: Int) {

        // Finds the holiday that matches the card selected by the user
        val selectedHoliday = holidays.find {
            it.holidayId == holidayId
        } ?: return

        selectedHolidayId = holidayId

        // Loads only the days belonging to the selected holiday
        itineraryDays =
            itineraryDaysByHoliday.getOrPut(holidayId) {
                mutableListOf()
            }

        // Loads only the activities belonging to the selected holiday
        itineraryActivities =
            activitiesByHoliday.getOrPut(holidayId) {
                mutableListOf()
            }

        // Selects the first available day when switching holidays
        selectedDayNumber =
            itineraryDays
                .minByOrNull { it.dayNumber }
                ?.dayNumber ?: -1

        // Updates the holiday heading and selected-card styling
        updateSelectedHolidayDetails(selectedHoliday)
        updateHolidayCardStyles()

        // Refreshes both the day selector and activity timeline
        renderDayCards()
        renderActivities()
    }

    private fun updateSelectedHolidayDetails(holiday: Holiday) {

        // Shows the name of whichever holiday is currently selected
        tvSelectedTripName.text = holiday.name

        val startDate = parseDate(holiday.startDate)
        val endDate = parseDate(holiday.endDate)

        if (startDate != null && endDate != null) {

            val startFormat =
                SimpleDateFormat(
                    "dd MMM",
                    Locale.getDefault()
                )

            val endFormat =
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )

            // Formats the dates to match the design, e.g. 12 Sep – 20 Sep 2026
            tvSelectedTripDates.text =
                "${startFormat.format(startDate)} – ${endFormat.format(endDate)}"

        } else {

            // Falls back to the stored dates if formatting fails
            tvSelectedTripDates.text =
                "${holiday.startDate} – ${holiday.endDate}"
        }
    }

    private fun updateHolidayCardStyles() {

        val holidayCards = listOf(
            1 to cardHoneymoon,
            2 to cardBusinessTrip,
            3 to cardFamilyVacation
        )

        holidayCards.forEach { (holidayId, card) ->

            if (holidayId == selectedHolidayId) {

                // Gives the selected holiday the blue outline shown in the design
                card.strokeColor =
                    Color.parseColor("#3D9BE9")

                card.strokeWidth =
                    dpToPx(2)

            } else {

                // Returns the other holiday cards to their normal grey outline
                card.strokeColor =
                    Color.parseColor("#E0E6EC")

                card.strokeWidth =
                    dpToPx(1)
            }
        }
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

// Refreshes both the selected day styling and its activity list
                    renderDayCards()
                    renderActivities()
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

    private fun renderActivities() {

        // Clears the old activity views before displaying the selected day's data
        activitiesContainer.removeAllViews()

        val selectedActivities = itineraryActivities
            .filter { it.dayNumber == selectedDayNumber }
            .sortedBy { it.startTime }

        if (selectedActivities.isEmpty()) {

            // Shows a simple message when the selected day has no activities yet
            val emptyMessage = TextView(requireContext()).apply {
                text = "No activities added for this day."
                textSize = 13f
                setTextColor(Color.parseColor("#7B8794"))
                setPadding(
                    0,
                    dpToPx(18),
                    0,
                    dpToPx(18)
                )
            }

            activitiesContainer.addView(emptyMessage)
            return
        }

        selectedActivities.forEach { activity ->

            val activityView =
                LayoutInflater.from(requireContext())
                    .inflate(
                        R.layout.item_itinerary_activity,
                        activitiesContainer,
                        false
                    )

            val tvTime =
                activityView.findViewById<TextView>(
                    R.id.tvActivityItemTime
                )

            val tvTitle =
                activityView.findViewById<TextView>(
                    R.id.tvActivityItemTitle
                )

            val btnEdit =
                activityView.findViewById<TextView>(
                    R.id.btnEditActivityItem
                )

            val btnDelete =
                activityView.findViewById<TextView>(
                    R.id.btnDeleteActivityItem
                )

            tvTime.text = activity.startTime
            tvTitle.text = activity.title

            // Edit and delete behaviour will be connected in the next steps
            btnEdit.setOnClickListener {

                // Opens the edit form for the selected activity
                showEditActivityDialog(activity)
            }

            btnDelete.setOnClickListener {

                // Opens a confirmation dialog before removing the selected activity
                showDeleteActivityDialog(activity)
            }

            activitiesContainer.addView(activityView)
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

// Refreshes the day cards and shows the empty activity state for the new day
                renderDayCards()
                renderActivities()

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

                    // Keeps the old day number so linked activities can be updated as well
                    val oldDayNumber = selectedDay.dayNumber

                    itineraryDays[index] =
                        ItineraryDay(
                            dayNumber = newDayNumber,
                            date = dateText
                        )

                    // Moves existing activities to the updated day number
                    for (activityIndex in itineraryActivities.indices) {

                        val activity = itineraryActivities[activityIndex]

                        if (activity.dayNumber == oldDayNumber) {

                            itineraryActivities[activityIndex] =
                                activity.copy(
                                    dayNumber = newDayNumber
                                )
                        }
                    }

                    selectedDayNumber = newDayNumber

                    // Refreshes both the day selector and the activities linked to this day
                    renderDayCards()
                    renderActivities()

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

            // Removes all activities that belonged to the deleted day
            itineraryActivities.removeAll {
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

            // Redraws the remaining days and their activity list
            renderDayCards()
            renderActivities()

            Toast.makeText(
                requireContext(),
                "Day ${selectedDay.dayNumber} deleted.",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddActivityDialog() {

        // Finds the currently selected day so the activity can be linked to it
        val selectedDay = itineraryDays.find {
            it.dayNumber == selectedDayNumber
        }

        // An activity cannot be added if the itinerary has no selected day
        if (selectedDay == null) {
            Toast.makeText(
                requireContext(),
                "Please select a day before adding an activity.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_activity, null)

        val layoutTime =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutActivityTime
            )

        val layoutTitle =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutActivityTitle
            )

        val etDay =
            dialogView.findViewById<TextInputEditText>(
                R.id.etActivityDay
            )

        val etTime =
            dialogView.findViewById<TextInputEditText>(
                R.id.etActivityTime
            )

        val etTitle =
            dialogView.findViewById<TextInputEditText>(
                R.id.etActivityTitle
            )

        val btnCancel =
            dialogView.findViewById<MaterialButton>(
                R.id.btnCancelActivity
            )

        val btnAdd =
            dialogView.findViewById<MaterialButton>(
                R.id.btnConfirmActivity
            )

        // Displays the selected itinerary day without allowing it to be manually changed
        val parsedDayDate = parseDate(selectedDay.date)

        val formattedDay =
            if (parsedDayDate != null) {

                SimpleDateFormat(
                    "dd MMM",
                    Locale.getDefault()
                ).format(parsedDayDate)

            } else {
                selectedDay.date
            }

        etDay.setText(
            "Day ${selectedDay.dayNumber} - $formattedDay"
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etTime.setOnClickListener {

            val calendar = Calendar.getInstance()

            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->

                    // Stores the time in 24-hour format so activities can be sorted correctly later
                    val selectedTime = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hourOfDay,
                        minute
                    )

                    etTime.setText(selectedTime)
                    layoutTime.error = null
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )

            timePicker.show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnAdd.setOnClickListener {

            // Clears old validation messages before checking the current values
            layoutTime.error = null
            layoutTitle.error = null

            val timeText =
                etTime.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val titleText =
                etTitle.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            var isValid = true

            if (timeText.isEmpty()) {
                layoutTime.error =
                    "Please select an activity time."
                isValid = false
            }

            if (titleText.isEmpty()) {
                layoutTitle.error =
                    "Please enter an activity title."
                isValid = false
            }

            if (isValid) {

                // Generates a temporary local ID until activity IDs come from the backend
                val nextActivityId =
                    (itineraryActivities.maxOfOrNull { it.activityId } ?: 0) + 1

                val newActivity = ItineraryActivity(
                    activityId = nextActivityId,
                    dayNumber = selectedDay.dayNumber,
                    title = titleText,
                    startTime = timeText
                )

                itineraryActivities.add(newActivity)

                // Refreshes the list so the new activity appears in chronological order
                renderActivities()

                Toast.makeText(
                    requireContext(),
                    "$titleText added.",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditActivityDialog(activity: ItineraryActivity) {

        // Opens the existing activity form with the selected activity's current values
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_activity, null)

        val tvTitle =
            dialogView.findViewById<TextView>(
                R.id.tvActivityDialogTitle
            )

        val tvSubtitle =
            dialogView.findViewById<TextView>(
                R.id.tvActivityDialogSubtitle
            )

        val layoutTime =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutActivityTime
            )

        val layoutTitle =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutActivityTitle
            )

        val etDay =
            dialogView.findViewById<TextInputEditText>(
                R.id.etActivityDay
            )

        val etTime =
            dialogView.findViewById<TextInputEditText>(
                R.id.etActivityTime
            )

        val etTitle =
            dialogView.findViewById<TextInputEditText>(
                R.id.etActivityTitle
            )

        val btnCancel =
            dialogView.findViewById<MaterialButton>(
                R.id.btnCancelActivity
            )

        val btnSave =
            dialogView.findViewById<MaterialButton>(
                R.id.btnConfirmActivity
            )

        // Changes the Add Activity dialog into Edit Activity mode
        tvTitle.text = "Edit Activity"
        tvSubtitle.text = "Update this activity in your itinerary."
        btnSave.text = "Save Changes"

        val selectedDay = itineraryDays.find {
            it.dayNumber == activity.dayNumber
        }

        val formattedDay =
            selectedDay?.let { day ->

                val parsedDate = parseDate(day.date)

                if (parsedDate != null) {
                    SimpleDateFormat(
                        "dd MMM",
                        Locale.getDefault()
                    ).format(parsedDate)
                } else {
                    day.date
                }

            } ?: ""

        etDay.setText(
            "Day ${activity.dayNumber} - $formattedDay"
        )

        // Pre-fills the activity information so the user can edit it
        etTime.setText(activity.startTime)
        etTitle.setText(activity.title)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etTime.setOnClickListener {

            val calendar = Calendar.getInstance()

            val timeParts = activity.startTime.split(":")

            if (timeParts.size == 2) {
                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    timeParts[0].toIntOrNull()
                        ?: calendar.get(Calendar.HOUR_OF_DAY)
                )

                calendar.set(
                    Calendar.MINUTE,
                    timeParts[1].toIntOrNull()
                        ?: calendar.get(Calendar.MINUTE)
                )
            }

            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->

                    val selectedTime = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hourOfDay,
                        minute
                    )

                    etTime.setText(selectedTime)
                    layoutTime.error = null
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )

            timePicker.show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {

            // Clears previous validation errors before checking the edited values
            layoutTime.error = null
            layoutTitle.error = null

            val newTime =
                etTime.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val newTitle =
                etTitle.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            var isValid = true

            if (newTime.isEmpty()) {
                layoutTime.error =
                    "Please select an activity time."
                isValid = false
            }

            if (newTitle.isEmpty()) {
                layoutTitle.error =
                    "Please enter an activity title."
                isValid = false
            }

            if (isValid) {

                val index =
                    itineraryActivities.indexOfFirst {
                        it.activityId == activity.activityId
                    }

                if (index != -1) {

                    // Replaces the old activity with the user's updated values
                    itineraryActivities[index] =
                        activity.copy(
                            title = newTitle,
                            startTime = newTime
                        )

                    // Redraws the activities so any changed time is re-sorted correctly
                    renderActivities()

                    Toast.makeText(
                        requireContext(),
                        "Activity updated successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteActivityDialog(activity: ItineraryActivity) {

        // Uses the same confirmation layout as Delete Day to keep dialogs consistent
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

        // Shows the activity name so the user knows exactly what will be removed
        tvTitle.text = "Delete Activity?"

        tvMessage.text =
            "Are you sure you want to delete \"${activity.title}\"?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            // Removes the activity using its unique local ID
            itineraryActivities.removeAll {
                it.activityId == activity.activityId
            }

            // Refreshes the selected day's activities immediately after deletion
            renderActivities()

            Toast.makeText(
                requireContext(),
                "${activity.title} deleted.",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteItineraryDialog() {

        // Finds the selected holiday so its name can be shown in the confirmation message
        val selectedHoliday = holidays.find {
            it.holidayId == selectedHolidayId
        } ?: return

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

        tvTitle.text = "Delete Itinerary?"

        tvMessage.text =
            "Are you sure you want to delete the ${selectedHoliday.name} itinerary? " +
                    "All days and activities in this itinerary will be removed."

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            // Clears only the itinerary data for the selected holiday
            itineraryDays.clear()
            itineraryActivities.clear()

            // No day remains selected once the itinerary has been cleared
            selectedDayNumber = -1

            // Refreshes the screen to show the empty itinerary state
            renderDayCards()
            renderActivities()

            Toast.makeText(
                requireContext(),
                "${selectedHoliday.name} itinerary deleted.",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isDateWithinHoliday(
        dateText: String
    ): Boolean {

        // Gets the start and end dates from the holiday currently selected
        val selectedHoliday = holidays.find {
            it.holidayId == selectedHolidayId
        } ?: return false

        val selectedDate =
            parseDate(dateText) ?: return false

        val holidayStart =
            parseDate(selectedHoliday.startDate) ?: return false

        val holidayEnd =
            parseDate(selectedHoliday.endDate) ?: return false

        // A new itinerary day must fall inside the selected holiday's date range
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