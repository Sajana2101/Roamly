package com.example.roamly.ui.holiday

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class EditHolidayFragment :
    Fragment(R.layout.fragment_edit_holiday) {

    private lateinit var holidayRepository:
            HolidayRepository

    private var holidayId: Int = -1

    private var selectedImageUri:
            Uri? = null

    private lateinit var currentHoliday:
            Holiday

    private lateinit var nameLayout:
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

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

    private val imagePicker =
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
            } catch (_: Exception) {
            }

            selectedImageUri = uri

            coverImageView
                .setImageURI(uri)
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        holidayId =
            arguments?.getInt(
                ARG_HOLIDAY_ID,
                -1
            ) ?: -1
    }

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

        bindViews(view)
        setupTripTypes()
        setupDates()
        loadHoliday()
        setupButtons(view)
    }

    private fun bindViews(
        view: View
    ) {
        nameLayout =
            view.findViewById(
                R.id.editHolidayNameLayout
            )

        countryLayout =
            view.findViewById(
                R.id.editCountryLayout
            )

        cityLayout =
            view.findViewById(
                R.id.editCityLayout
            )

        startDateLayout =
            view.findViewById(
                R.id.editStartDateLayout
            )

        endDateLayout =
            view.findViewById(
                R.id.editEndDateLayout
            )

        nameEditText =
            view.findViewById(
                R.id.editHolidayNameEditText
            )

        tripTypeAutoComplete =
            view.findViewById(
                R.id.editTripTypeAutoComplete
            )

        countryEditText =
            view.findViewById(
                R.id.editCountryEditText
            )

        cityEditText =
            view.findViewById(
                R.id.editCityEditText
            )

        startDateEditText =
            view.findViewById(
                R.id.editStartDateEditText
            )

        endDateEditText =
            view.findViewById(
                R.id.editEndDateEditText
            )

        coverImageView =
            view.findViewById(
                R.id.editCoverImageView
            )
    }

    private fun setupTripTypes() {
        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
                resources.getStringArray(
                    R.array.trip_types
                )
            )

        tripTypeAutoComplete
            .setAdapter(adapter)
    }

    private fun setupDates() {
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
        target: TextInputEditText
    ) {
        val calendar =
            Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            {
                    _,
                    year,
                    month,
                    day ->

                calendar.set(
                    year,
                    month,
                    day
                )

                target.setText(
                    dateFormat.format(
                        calendar.time
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
        ).show()
    }

    private fun loadHoliday() {
        val holiday =
            holidayRepository
                .getHolidayById(
                    holidayId
                )
                ?: return

        currentHoliday =
            holiday

        nameEditText.setText(
            holiday.name
        )

        tripTypeAutoComplete.setText(
            holiday.tripType,
            false
        )

        countryEditText.setText(
            holiday.country
        )

        cityEditText.setText(
            holiday.city
        )

        startDateEditText.setText(
            holiday.startDate
        )

        endDateEditText.setText(
            holiday.endDate
        )

        if (
            !holiday.coverImageUri
                .isNullOrBlank()
        ) {
            coverImageView.setImageURI(
                Uri.parse(
                    holiday.coverImageUri
                )
            )
        }
    }

    private fun setupButtons(
        view: View
    ) {
        view.findViewById<MaterialButton>(
            R.id.changeCoverImageButton
        ).setOnClickListener {

            imagePicker.launch(
                arrayOf(
                    "image/*"
                )
            )
        }

        view.findViewById<MaterialButton>(
            R.id.saveHolidayChangesButton
        ).setOnClickListener {

            saveChanges()
        }
    }

    private fun saveChanges() {
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

        val start =
            startDateEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val end =
            endDateEditText.text
                ?.toString()
                ?.trim()
                .orEmpty()

        var valid = true

        if (name.isBlank()) {
            nameLayout.error =
                getString(
                    R.string.field_required
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

        if (start.isBlank()) {
            startDateLayout.error =
                getString(
                    R.string.select_start_date
                )

            valid = false
        }

        if (end.isBlank()) {
            endDateLayout.error =
                getString(
                    R.string.select_end_date
                )

            valid = false
        }

        if (
            start.isNotBlank() &&
            end.isNotBlank()
        ) {
            val startDate =
                dateFormat.parse(start)

            val endDate =
                dateFormat.parse(end)

            if (
                startDate != null &&
                endDate != null &&
                endDate.before(
                    startDate
                )
            ) {
                endDateLayout.error =
                    getString(
                        R.string.end_date_before_start
                    )

                valid = false
            }
        }

        if (!valid) {
            return
        }

        val image =
            selectedImageUri
                ?.toString()
                ?: currentHoliday
                    .coverImageUri

        val updatedHoliday =
            currentHoliday.copy(
                name = name,
                location =
                    "$city, $country",
                tripType = tripType,
                country = country,
                city = city,
                startDate = start,
                endDate = end,
                coverImageUri = image
            )

        holidayRepository
            .updateHoliday(
                updatedHoliday
            )

        Toast.makeText(
            requireContext(),
            getString(
                R.string.holiday_updated
            ),
            Toast.LENGTH_SHORT
        ).show()

        (
                requireActivity()
                        as MainActivity
                )
            .showHolidayDetails(
                holidayId
            )
    }

    private fun clearErrors() {
        nameLayout.error = null
        countryLayout.error = null
        cityLayout.error = null
        startDateLayout.error = null
        endDateLayout.error = null
    }

    companion object {
        private const val ARG_HOLIDAY_ID =
            "holiday_id"

        fun newInstance(
            holidayId: Int
        ): EditHolidayFragment {
            return EditHolidayFragment()
                .apply {
                    arguments =
                        Bundle().apply {
                            putInt(
                                ARG_HOLIDAY_ID,
                                holidayId
                            )
                        }
                }
        }
    }
}