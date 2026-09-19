package com.example.roamly.ui.holiday

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.repository.HolidayRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddHolidayFragment :
    Fragment(R.layout.fragment_add_holiday) {

    private lateinit var holidayRepository:
            HolidayRepository

    private lateinit var nameLayout:
            TextInputLayout

    private lateinit var tripTypeLayout:
            TextInputLayout

    private lateinit var countryLayout:
            TextInputLayout

    private lateinit var cityLayout:
            TextInputLayout

    private lateinit var startDateLayout:
            TextInputLayout

    private lateinit var endDateLayout:
            TextInputLayout

    private lateinit var nameEditText:
            TextInputEditText

    private lateinit var tripTypeAutoComplete:
            AutoCompleteTextView

    private lateinit var countryEditText:
            TextInputEditText

    private lateinit var cityEditText:
            TextInputEditText

    private lateinit var startDateEditText:
            TextInputEditText

    private lateinit var endDateEditText:
            TextInputEditText

    private lateinit var coverImageView:
            ImageView

    private lateinit var selectCoverImageButton:
            MaterialButton

    private lateinit var createHolidayButton:
            MaterialButton

    private var selectedCoverImageUri:
            Uri? = null

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

    private val coverImagePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            try {
                requireContext()
                    .contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
            } catch (
                exception: SecurityException
            ) {
                Log.w(
                    "RoamlyHoliday",
                    "Could not persist cover image permission",
                    exception
                )
            }

            selectedCoverImageUri = uri

            coverImageView.setImageURI(uri)

            coverImageView.visibility =
                View.VISIBLE

            selectCoverImageButton.text =
                getString(
                    R.string.change_cover_image
                )
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        dateFormat.isLenient = false

        holidayRepository =
            HolidayRepository(
                requireContext()
                    .applicationContext
            )

        bindViews(view)
        setupTripTypes()
        setupDatePickers()
        setupCoverImagePicker()
        setupCreateButton()
    }

    private fun bindViews(
        view: View
    ) {
        nameLayout =
            view.findViewById(
                R.id.holidayNameLayout
            )

        tripTypeLayout =
            view.findViewById(
                R.id.tripTypeLayout
            )

        countryLayout =
            view.findViewById(
                R.id.countryLayout
            )

        cityLayout =
            view.findViewById(
                R.id.cityLayout
            )

        startDateLayout =
            view.findViewById(
                R.id.startDateLayout
            )

        endDateLayout =
            view.findViewById(
                R.id.endDateLayout
            )

        nameEditText =
            view.findViewById(
                R.id.holidayNameEditText
            )

        tripTypeAutoComplete =
            view.findViewById(
                R.id.tripTypeAutoComplete
            )

        countryEditText =
            view.findViewById(
                R.id.countryEditText
            )

        cityEditText =
            view.findViewById(
                R.id.cityEditText
            )

        startDateEditText =
            view.findViewById(
                R.id.startDateEditText
            )

        endDateEditText =
            view.findViewById(
                R.id.endDateEditText
            )

        coverImageView =
            view.findViewById(
                R.id.holidayCoverImageView
            )

        selectCoverImageButton =
            view.findViewById(
                R.id.selectCoverImageButton
            )

        createHolidayButton =
            view.findViewById(
                R.id.createHolidayButton
            )
    }

    private fun setupTripTypes() {
        val tripTypes =
            resources.getStringArray(
                R.array.trip_types
            )

        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
                tripTypes
            )

        tripTypeAutoComplete
            .setAdapter(adapter)
    }

    private fun setupDatePickers() {
        startDateEditText
            .setOnClickListener {
                showDatePicker(
                    startDateEditText
                )
            }

        endDateEditText
            .setOnClickListener {
                showDatePicker(
                    endDateEditText
                )
            }
    }

    private fun showDatePicker(
        target:
        TextInputEditText
    ) {
        val calendar =
            Calendar.getInstance()

        val dialog =
            DatePickerDialog(
                requireContext(),
                {
                        _,
                        year,
                        month,
                        dayOfMonth ->

                    val selectedDate =
                        Calendar
                            .getInstance()

                    selectedDate.set(
                        year,
                        month,
                        dayOfMonth
                    )

                    target.setText(
                        dateFormat.format(
                            selectedDate.time
                        )
                    )
                },
                calendar.get(
                    Calendar.YEAR
                ),
                calendar.get(
                    Calendar.MONTH
                ),
                calendar.get(
                    Calendar.DAY_OF_MONTH
                )
            )

        dialog.show()
    }

    private fun setupCoverImagePicker() {
        selectCoverImageButton
            .setOnClickListener {
                coverImagePicker.launch(
                    arrayOf(
                        "image/*"
                    )
                )
            }
    }

    private fun setupCreateButton() {
        createHolidayButton
            .setOnClickListener {
                createHoliday()
            }
    }

    private fun createHoliday() {
        clearErrors()

        val name =
            nameEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val tripType =
            tripTypeAutoComplete.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val country =
            countryEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val city =
            cityEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val startDate =
            startDateEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val endDate =
            endDateEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (
            !validateInput(
                name,
                tripType,
                country,
                city,
                startDate,
                endDate
            )
        ) {
            return
        }

        val holiday =
            Holiday(
                holidayId =
                    holidayRepository
                        .getNextHolidayId(),
                name = name,
                location =
                    "$city, $country",
                startDate = startDate,
                endDate = endDate,
                tripType = tripType,
                country = country,
                city = city,
                coverImageUri =
                    selectedCoverImageUri
                        ?.toString()
            )

        holidayRepository
            .addHoliday(holiday)

        Log.i(
            "RoamlyHoliday",
            "Holiday created: ${holiday.holidayId}"
        )

        Toast.makeText(
            requireContext(),
            getString(
                R.string.holiday_created
            ),
            Toast.LENGTH_SHORT
        ).show()

        (
                requireActivity()
                        as MainActivity
                )
            .showHome()
    }

    private fun validateInput(
        name: String,
        tripType: String,
        country: String,
        city: String,
        startDate: String,
        endDate: String
    ): Boolean {

        var valid = true

        if (name.isBlank()) {
            nameLayout.error =
                getString(
                    R.string.field_required
                )

            valid = false
        }

        if (tripType.isBlank()) {
            tripTypeLayout.error =
                getString(
                    R.string.select_trip_type
                )

            valid = false
        }

        if (country.isBlank()) {
            countryLayout.error =
                getString(
                    R.string.field_required
                )

            valid = false
        }

        if (city.isBlank()) {
            cityLayout.error =
                getString(
                    R.string.field_required
                )

            valid = false
        }

        if (startDate.isBlank()) {
            startDateLayout.error =
                getString(
                    R.string.select_start_date
                )

            valid = false
        }

        if (endDate.isBlank()) {
            endDateLayout.error =
                getString(
                    R.string.select_end_date
                )

            valid = false
        }

        if (
            startDate.isNotBlank() &&
            endDate.isNotBlank()
        ) {
            try {
                val parsedStart =
                    dateFormat.parse(
                        startDate
                    )

                val parsedEnd =
                    dateFormat.parse(
                        endDate
                    )

                if (
                    parsedStart != null &&
                    parsedEnd != null &&
                    parsedEnd.before(
                        parsedStart
                    )
                ) {
                    endDateLayout.error =
                        getString(
                            R.string
                                .end_date_before_start
                        )

                    valid = false
                }
            } catch (
                exception: Exception
            ) {
                Log.e(
                    "RoamlyHoliday",
                    "Holiday date validation failed",
                    exception
                )

                valid = false
            }
        }

        return valid
    }

    private fun clearErrors() {
        nameLayout.error = null
        tripTypeLayout.error = null
        countryLayout.error = null
        cityLayout.error = null
        startDateLayout.error = null
        endDateLayout.error = null
    }
}