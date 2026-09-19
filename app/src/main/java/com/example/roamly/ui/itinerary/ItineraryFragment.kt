package com.example.roamly.ui.itinerary

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.roamly.R
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import com.example.roamly.data.repository.HolidayRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ItineraryFragment : Fragment(R.layout.fragment_itinerary) {

    private lateinit var holidayRepository: HolidayRepository
    private lateinit var holidaysContainer: LinearLayout
    private lateinit var daysContainer: LinearLayout
    private lateinit var tvSelectedDayDate: TextView
    private lateinit var activitiesContainer: LinearLayout
    private lateinit var tvSelectedTripName: TextView
    private lateinit var tvSelectedTripDates: TextView

    private val holidays = mutableListOf<Holiday>()
    private val holidayCards = linkedMapOf<Int, MaterialCardView>()

    private var selectedHolidayId = -1

    private val itineraryDaysByHoliday =
        mutableMapOf<Int, MutableList<ItineraryDay>>()

    private val activitiesByHoliday =
        mutableMapOf<Int, MutableList<ItineraryActivity>>()

    private var itineraryDays =
        mutableListOf<ItineraryDay>()

    private var itineraryActivities =
        mutableListOf<ItineraryActivity>()

    private var selectedDayNumber = -1

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        holidayRepository =
            HolidayRepository(
                requireContext()
                    .applicationContext
            )

        holidaysContainer =
            view.findViewById(
                R.id.holidaysContainer
            )

        daysContainer =
            view.findViewById(
                R.id.daysContainer
            )

        tvSelectedDayDate =
            view.findViewById(
                R.id.tvSelectedDayDate
            )

        activitiesContainer =
            view.findViewById(
                R.id.activitiesContainer
            )

        tvSelectedTripName =
            view.findViewById(
                R.id.tvSelectedTripName
            )

        tvSelectedTripDates =
            view.findViewById(
                R.id.tvSelectedTripDates
            )

        view.findViewById<MaterialButton>(
            R.id.btnAddDay
        ).setOnClickListener {
            showAddDayDialog()
        }

        view.findViewById<MaterialButton>(
            R.id.btnEditDay
        ).setOnClickListener {
            showEditDayDialog()
        }

        view.findViewById<MaterialButton>(
            R.id.btnDeleteDay
        ).setOnClickListener {
            showDeleteDayDialog()
        }

        view.findViewById<MaterialButton>(
            R.id.btnAddActivity
        ).setOnClickListener {
            showAddActivityDialog()
        }

        view.findViewById<MaterialButton>(
            R.id.btnDeleteItinerary
        ).setOnClickListener {
            showDeleteItineraryDialog()
        }

        reloadHolidays()
    }

    override fun onResume() {
        super.onResume()

        if (::holidayRepository.isInitialized) {
            reloadHolidays()
        }
    }

    private fun reloadHolidays() {
        val loadedHolidays =
            holidayRepository
                .getHolidays()
                .sortedBy {
                    it.startDate
                }

        holidays.clear()
        holidays.addAll(
            loadedHolidays
        )

        val validHolidayIds =
            holidays
                .map {
                    it.holidayId
                }
                .toSet()

        itineraryDaysByHoliday
            .keys
            .retainAll(
                validHolidayIds
            )

        activitiesByHoliday
            .keys
            .retainAll(
                validHolidayIds
            )

        renderHolidayCards()

        if (holidays.isEmpty()) {
            showNoHolidayState()
            return
        }

        if (
            holidays.none {
                it.holidayId ==
                        selectedHolidayId
            }
        ) {
            selectedHolidayId =
                holidays.first()
                    .holidayId
        }

        selectHoliday(
            selectedHolidayId
        )
    }

    private fun renderHolidayCards() {
        holidaysContainer.removeAllViews()
        holidayCards.clear()

        if (holidays.isEmpty()) {
            val emptyMessage =
                TextView(
                    requireContext()
                ).apply {
                    text =
                        "No holidays yet. Create a holiday first."
                    textSize =
                        13f
                    setTextColor(
                        Color.parseColor(
                            "#7B8794"
                        )
                    )
                    setPadding(
                        0,
                        dpToPx(14),
                        0,
                        dpToPx(14)
                    )
                }

            holidaysContainer.addView(
                emptyMessage
            )

            return
        }

        holidays.forEach { holiday ->
            val cardView =
                LayoutInflater
                    .from(
                        requireContext()
                    )
                    .inflate(
                        R.layout.item_itinerary_holiday,
                        holidaysContainer,
                        false
                    ) as MaterialCardView

            val imageView =
                cardView.findViewById<ImageView>(
                    R.id.imgItineraryHoliday
                )

            val nameTextView =
                cardView.findViewById<TextView>(
                    R.id.tvItineraryHolidayName
                )

            val locationTextView =
                cardView.findViewById<TextView>(
                    R.id.tvItineraryHolidayLocation
                )

            nameTextView.text =
                holiday.name

            locationTextView.text =
                holiday.location

            loadHolidayImage(
                imageView,
                holiday
            )

            cardView.setOnClickListener {
                selectHoliday(
                    holiday.holidayId
                )
            }

            holidayCards[
                holiday.holidayId
            ] = cardView

            holidaysContainer.addView(
                cardView
            )
        }
    }

    private fun loadHolidayImage(
        imageView: ImageView,
        holiday: Holiday
    ) {
        val imageUri =
            holiday.coverImageUri

        if (imageUri.isNullOrBlank()) {
            imageView.setImageResource(
                R.drawable.ic_holiday_placeholder
            )
            return
        }

        try {
            imageView.setImageURI(
                Uri.parse(
                    imageUri
                )
            )
        } catch (
            exception: Exception
        ) {
            imageView.setImageResource(
                R.drawable.ic_holiday_placeholder
            )
        }
    }

    private fun showNoHolidayState() {
        selectedHolidayId =
            -1

        itineraryDays =
            mutableListOf()

        itineraryActivities =
            mutableListOf()

        selectedDayNumber =
            -1

        tvSelectedTripName.text =
            "No holidays yet"

        tvSelectedTripDates.text =
            "Create a holiday first to build an itinerary."

        renderDayCards()
        renderActivities()
        updateHolidayCardStyles()

        setItineraryActionsEnabled(
            false
        )
    }

    private fun setItineraryActionsEnabled(
        enabled: Boolean
    ) {
        view?.findViewById<MaterialButton>(
            R.id.btnAddDay
        )?.isEnabled =
            enabled

        view?.findViewById<MaterialButton>(
            R.id.btnEditDay
        )?.isEnabled =
            enabled

        view?.findViewById<MaterialButton>(
            R.id.btnDeleteDay
        )?.isEnabled =
            enabled

        view?.findViewById<MaterialButton>(
            R.id.btnAddActivity
        )?.isEnabled =
            enabled

        view?.findViewById<MaterialButton>(
            R.id.btnDeleteItinerary
        )?.isEnabled =
            enabled
    }

    private fun selectHoliday(
        holidayId: Int
    ) {
        val selectedHoliday =
            holidays.find {
                it.holidayId ==
                        holidayId
            } ?: return

        selectedHolidayId =
            holidayId

        itineraryDays =
            itineraryDaysByHoliday
                .getOrPut(
                    holidayId
                ) {
                    mutableListOf()
                }

        itineraryActivities =
            activitiesByHoliday
                .getOrPut(
                    holidayId
                ) {
                    mutableListOf()
                }

        selectedDayNumber =
            itineraryDays
                .minByOrNull {
                    it.dayNumber
                }
                ?.dayNumber
                ?: -1

        setItineraryActionsEnabled(
            true
        )

        updateSelectedHolidayDetails(
            selectedHoliday
        )

        updateHolidayCardStyles()

        renderDayCards()
        renderActivities()
    }

    private fun updateSelectedHolidayDetails(
        holiday: Holiday
    ) {
        tvSelectedTripName.text =
            holiday.name

        val startDate =
            parseDate(
                holiday.startDate
            )

        val endDate =
            parseDate(
                holiday.endDate
            )

        if (
            startDate != null &&
            endDate != null
        ) {
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

            tvSelectedTripDates.text =
                "${startFormat.format(startDate)} – ${endFormat.format(endDate)}"
        } else {
            tvSelectedTripDates.text =
                "${holiday.startDate} – ${holiday.endDate}"
        }
    }

    private fun updateHolidayCardStyles() {
        holidayCards
            .forEach {
                    (
                        holidayId,
                        card
                    ) ->

                if (
                    holidayId ==
                    selectedHolidayId
                ) {
                    card.strokeColor =
                        Color.parseColor(
                            "#3D9BE9"
                        )

                    card.strokeWidth =
                        dpToPx(
                            2
                        )
                } else {
                    card.strokeColor =
                        Color.parseColor(
                            "#E0E6EC"
                        )

                    card.strokeWidth =
                        dpToPx(
                            1
                        )
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
                activityView.findViewById<MaterialButton>(
                    R.id.btnEditActivityItem
                )

            val btnDelete =
                activityView.findViewById<MaterialButton>(
                    R.id.btnDeleteActivityItem
                )

// References the timeline icon so each type of activity can use a suitable symbol
            val activityIcon =
                activityView.findViewById<ImageView>(
                    R.id.imgActivityTypeIcon
                )

            val activityIconCard =
                activityView.findViewById<MaterialCardView>(
                    R.id.cardActivityTypeIcon
                )

            tvTime.text = activity.startTime
            tvTitle.text = activity.title

            // Applies an icon and colour based on the activity being displayed
            applyActivityIconStyle(
                activity.title,
                activityIcon,
                activityIconCard
            )

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

    private fun applyActivityIconStyle(
        title: String,
        iconView: ImageView,
        iconCard: MaterialCardView
    ) {

        // Uses the activity name to choose a suitable visual icon for the timeline
        val activityTitle =
            title.lowercase(Locale.getDefault())

        when {

            activityTitle.contains("flight") ||
                    activityTitle.contains("airport") -> {

                iconView.setImageResource(
                    R.drawable.ic_flight_takeoff
                )

                iconView.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#2F8FD8")
                    )

                iconCard.setCardBackgroundColor(
                    Color.parseColor("#E5F2FD")
                )
            }

            activityTitle.contains("hotel") ||
                    activityTitle.contains("check-in") ||
                    activityTitle.contains("check in") -> {

                iconView.setImageResource(
                    R.drawable.ic_hotel
                )

                iconView.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#3E9B7C")
                    )

                iconCard.setCardBackgroundColor(
                    Color.parseColor("#E5F5EF")
                )
            }

            activityTitle.contains("breakfast") ||
                    activityTitle.contains("lunch") ||
                    activityTitle.contains("dinner") ||
                    activityTitle.contains("restaurant") ||
                    activityTitle.contains("food") ||
                    activityTitle.contains("cafe") ||
                    activityTitle.contains("coffee") -> {

                iconView.setImageResource(
                    R.drawable.ic_restaurant
                )

                iconView.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#D99032")
                    )

                iconCard.setCardBackgroundColor(
                    Color.parseColor("#FFF2DF")
                )
            }

            else -> {

                // Uses a general location icon when no specific activity type is recognised
                iconView.setImageResource(
                    R.drawable.ic_place
                )

                iconView.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#7867B8")
                    )

                iconCard.setCardBackgroundColor(
                    Color.parseColor("#EEEAF8")
                )
            }
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

    private fun parseDate(
        dateText: String
    ) = try {
        val supportedFormats =
            listOf(
                "yyyy-MM-dd",
                "yyyy/MM/dd"
            )

        supportedFormats
            .firstNotNullOfOrNull { pattern ->
                try {
                    SimpleDateFormat(
                        pattern,
                        Locale.getDefault()
                    ).apply {
                        isLenient = false
                    }.parse(
                        dateText
                    )
                } catch (
                    exception: Exception
                ) {
                    null
                }
            }
    } catch (
        exception: Exception
    ) {
        null
    }

    private fun dpToPx(dp: Int): Int {
        return (
                dp * resources.displayMetrics.density
                ).toInt()
    }
}
