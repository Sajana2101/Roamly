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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.roamly.R
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.sync.HolidayCoverImage
import com.example.roamly.data.model.ItineraryActivity
import com.example.roamly.data.model.ItineraryDay
import com.example.roamly.data.repository.RoomHolidayRepository
import com.example.roamly.data.repository.ItineraryRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ItineraryFragment : Fragment(R.layout.fragment_itinerary) {

    private lateinit var holidayRepository: RoomHolidayRepository
    private lateinit var itineraryRepository: ItineraryRepository
    private lateinit var holidaysContainer: LinearLayout
    private lateinit var daysContainer: LinearLayout
    private lateinit var tvSelectedDayDate: TextView
    private lateinit var activitiesContainer: LinearLayout
    private lateinit var tvSelectedTripName: TextView
    private lateinit var tvSelectedTripDates: TextView

    private val holidays = mutableListOf<Holiday>()
    private val holidayCards = linkedMapOf<Int, MaterialCardView>()

    private var selectedHolidayId = -1

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
            RoomHolidayRepository(
                requireContext()
                    .applicationContext
            )

        itineraryRepository =
            ItineraryRepository(
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
        viewLifecycleOwner.lifecycleScope.launch {
            try {

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

                renderHolidayCards()

                if (holidays.isEmpty()) {
                    showNoHolidayState()
                    return@launch
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

            } catch (error: Exception) {
                android.util.Log.e(
                    "RoamlyRoom",
                    "Could not load holidays",
                    error
                )

                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.itinerary_load_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                        getString(R.string.itinerary_no_holidays)

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
        HolidayCoverImage.load(
            imageView,
            holiday
        )
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
            getString(R.string.itinerary_no_holidays_title)

        tvSelectedTripDates.text =
            getString(R.string.itinerary_create_holiday_message)

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
            itineraryRepository
                .getItineraryDays(
                    holidayId
                )

        itineraryActivities =
            itineraryRepository
                .getItineraryActivities(
                    holidayId
                )

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

                tvDayNumber.text =
                    getString(
                        R.string.itinerary_day_number,
                        day.dayNumber
                    )

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
                    renderActivities()
                }

                daysContainer.addView(dayView)
            }

        itineraryDays
            .find { it.dayNumber == selectedDayNumber }
            ?.let {
                updateSelectedDayHeading(it)
            }

        if (itineraryDays.isEmpty()) {
            tvSelectedDayDate.text =
                getString(R.string.itinerary_no_day_selected)
        }
    }

    private fun renderActivities() {

        activitiesContainer.removeAllViews()

        val selectedActivities = itineraryActivities
            .filter { it.dayNumber == selectedDayNumber }
            .sortedBy { it.startTime }

        if (selectedActivities.isEmpty()) {

            val emptyMessage = TextView(requireContext()).apply {
                text =
                    getString(R.string.itinerary_no_activities)

                textSize = 13f

                setTextColor(
                    Color.parseColor("#7B8794")
                )

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

            applyActivityIconStyle(
                activity.title,
                activityIcon,
                activityIconCard
            )

            btnEdit.setOnClickListener {
                showEditActivityDialog(activity)
            }

            btnDelete.setOnClickListener {
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
                    getString(R.string.itinerary_invalid_day_number)

                isValid = false

            } else if (
                itineraryDays.any {
                    it.dayNumber == dayNumber
                }
            ) {

                layoutDayNumber.error =
                    getString(R.string.itinerary_duplicate_day_number)

                isValid = false
            }

            if (dateText.isEmpty()) {

                layoutDate.error =
                    getString(R.string.itinerary_date_required)

                isValid = false

            } else if (!isDateWithinHoliday(dateText)) {

                layoutDate.error =
                    getString(R.string.itinerary_date_outside_holiday)

                isValid = false

            } else if (
                itineraryDays.any {
                    it.date == dateText
                }
            ) {

                layoutDate.error =
                    getString(R.string.itinerary_duplicate_date)

                isValid = false
            }

            if (isValid && dayNumber != null) {

                val newDay = ItineraryDay(
                    dayNumber = dayNumber,
                    date = dateText
                )

                itineraryDays.add(newDay)

                saveCurrentItinerary()

                selectedDayNumber = newDay.dayNumber

                renderDayCards()
                renderActivities()

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.itinerary_day_added,
                        dayNumber
                    ),
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
                getString(R.string.itinerary_select_day_first),
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

        tvTitle.text =
            getString(R.string.edit_day)

        tvSubtitle.text =
            getString(R.string.edit_day_description)

        btnSave.text =
            getString(R.string.save_changes)

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
                    getString(R.string.itinerary_invalid_day_number)

                isValid = false

            } else if (
                itineraryDays.any {
                    it.dayNumber == newDayNumber &&
                            it.dayNumber != selectedDay.dayNumber
                }
            ) {

                layoutDayNumber.error =
                    getString(R.string.itinerary_duplicate_day_number)

                isValid = false
            }

            if (dateText.isEmpty()) {

                layoutDate.error =
                    getString(R.string.itinerary_date_required)

                isValid = false

            } else if (!isDateWithinHoliday(dateText)) {

                layoutDate.error =
                    getString(R.string.itinerary_date_outside_holiday)

                isValid = false

            } else if (
                itineraryDays.any {
                    it.date == dateText &&
                            it.dayNumber != selectedDay.dayNumber
                }
            ) {

                layoutDate.error =
                    getString(R.string.itinerary_duplicate_date)

                isValid = false
            }

            if (isValid && newDayNumber != null) {

                val index =
                    itineraryDays.indexOfFirst {
                        it.dayNumber == selectedDay.dayNumber
                    }

                if (index != -1) {

                    val oldDayNumber = selectedDay.dayNumber

                    itineraryDays[index] =
                        ItineraryDay(
                            dayNumber = newDayNumber,
                            date = dateText
                        )

                    for (activityIndex in itineraryActivities.indices) {

                        val activity = itineraryActivities[activityIndex]

                        if (activity.dayNumber == oldDayNumber) {

                            itineraryActivities[activityIndex] =
                                activity.copy(
                                    dayNumber = newDayNumber
                                )
                        }
                    }

                    saveCurrentItinerary()

                    selectedDayNumber = newDayNumber

                    renderDayCards()
                    renderActivities()

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.itinerary_day_updated),
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDayDialog() {

        val selectedDay = itineraryDays.find {
            it.dayNumber == selectedDayNumber
        }

        if (selectedDay == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.itinerary_select_day_first),
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

        tvTitle.text =
            getString(
                R.string.delete_day_title,
                selectedDay.dayNumber
            )

        tvMessage.text =
            getString(
                R.string.delete_day_confirmation,
                selectedDay.dayNumber
            )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            itineraryDays.removeAll {
                it.dayNumber == selectedDay.dayNumber
            }

            itineraryActivities.removeAll {
                it.dayNumber == selectedDay.dayNumber
            }

            saveCurrentItinerary()

            if (itineraryDays.isNotEmpty()) {

                selectedDayNumber =
                    itineraryDays
                        .minByOrNull { it.dayNumber }
                        ?.dayNumber ?: -1

            } else {

                selectedDayNumber = -1

                tvSelectedDayDate.text =
                    getString(R.string.itinerary_no_day_selected)
            }

            renderDayCards()
            renderActivities()

            Toast.makeText(
                requireContext(),
                getString(
                    R.string.itinerary_day_deleted,
                    selectedDay.dayNumber
                ),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddActivityDialog() {

        val selectedDay = itineraryDays.find {
            it.dayNumber == selectedDayNumber
        }

        if (selectedDay == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.itinerary_select_day_before_activity),
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
            getString(
                R.string.itinerary_day_date,
                getString(
                    R.string.itinerary_day_number,
                    selectedDay.dayNumber
                ),
                formattedDay
            )
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        etTime.setOnClickListener {

            val calendar = Calendar.getInstance()

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

        btnAdd.setOnClickListener {

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
                    getString(R.string.itinerary_time_required)

                isValid = false
            }

            if (titleText.isEmpty()) {
                layoutTitle.error =
                    getString(R.string.itinerary_activity_title_required)

                isValid = false
            }

            if (isValid) {

                val nextActivityId =
                    (itineraryActivities.maxOfOrNull { it.activityId } ?: 0) + 1

                val newActivity = ItineraryActivity(
                    activityId = nextActivityId,
                    dayNumber = selectedDay.dayNumber,
                    title = titleText,
                    startTime = timeText
                )

                itineraryActivities.add(newActivity)

                saveCurrentItinerary()

                renderActivities()

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.itinerary_activity_added,
                        titleText
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditActivityDialog(activity: ItineraryActivity) {

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

        tvTitle.text =
            getString(R.string.edit_activity)

        tvSubtitle.text =
            getString(R.string.edit_activity_description)

        btnSave.text =
            getString(R.string.save_changes)

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
            getString(
                R.string.itinerary_day_date,
                getString(
                    R.string.itinerary_day_number,
                    activity.dayNumber
                ),
                formattedDay
            )
        )

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
                    getString(R.string.itinerary_time_required)

                isValid = false
            }

            if (newTitle.isEmpty()) {
                layoutTitle.error =
                    getString(R.string.itinerary_activity_title_required)

                isValid = false
            }

            if (isValid) {

                val index =
                    itineraryActivities.indexOfFirst {
                        it.activityId == activity.activityId
                    }

                if (index != -1) {

                    itineraryActivities[index] =
                        activity.copy(
                            title = newTitle,
                            startTime = newTime
                        )

                    saveCurrentItinerary()

                    renderActivities()

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.itinerary_activity_updated),
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteActivityDialog(activity: ItineraryActivity) {

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

        tvTitle.text =
            getString(R.string.delete_activity_title)

        tvMessage.text =
            getString(
                R.string.delete_activity_confirmation,
                activity.title
            )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            itineraryActivities.removeAll {
                it.activityId == activity.activityId
            }

            saveCurrentItinerary()

            renderActivities()

            Toast.makeText(
                requireContext(),
                getString(
                    R.string.itinerary_activity_deleted,
                    activity.title
                ),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteItineraryDialog() {

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

        tvTitle.text =
            getString(R.string.delete_itinerary_title)

        tvMessage.text =
            getString(
                R.string.delete_itinerary_confirmation,
                selectedHoliday.name
            )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {

            itineraryDays.clear()
            itineraryActivities.clear()

            itineraryRepository
                .deleteItinerary(
                    selectedHolidayId
                )

            selectedDayNumber = -1

            renderDayCards()
            renderActivities()

            Toast.makeText(
                requireContext(),
                getString(
                    R.string.itinerary_deleted,
                    selectedHoliday.name
                ),
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveCurrentItinerary() {

        if (selectedHolidayId == -1) {
            return
        }

        itineraryRepository
            .saveItineraryDays(
                selectedHolidayId,
                itineraryDays
            )

        itineraryRepository
            .saveItineraryActivities(
                selectedHolidayId,
                itineraryActivities
            )
    }

    private fun isDateWithinHoliday(
        dateText: String
    ): Boolean {

        val selectedHoliday = holidays.find {
            it.holidayId == selectedHolidayId
        } ?: return false

        val selectedDate =
            parseDate(dateText) ?: return false

        val holidayStart =
            parseDate(selectedHoliday.startDate) ?: return false

        val holidayEnd =
            parseDate(selectedHoliday.endDate) ?: return false

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