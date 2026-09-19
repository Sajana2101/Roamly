package com.example.roamly.ui.holiday

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.roamly.data.model.AccommodationDetails
import com.example.roamly.data.model.FlightDetails
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.repository.HolidayRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
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

    private lateinit var flyingSwitch:
            SwitchMaterial

    private lateinit var flightDetailsContainer:
            View

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

        setupHolidayDatePickers()

        setupTravelDateAndTimePickers(
            view
        )

        setupFlyingSwitch(
            view
        )

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

        flyingSwitch =
            view.findViewById(
                R.id.flyingSwitch
            )

        flightDetailsContainer =
            view.findViewById(
                R.id.flightDetailsContainer
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

    private fun setupHolidayDatePickers() {
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

    private fun setupTravelDateAndTimePickers(
        view: View
    ) {
        val dateFields =
            listOf(
                R.id.accommodationCheckInDateEditText,
                R.id.accommodationCheckOutDateEditText,
                R.id.outboundFlightDateEditText,
                R.id.returnFlightDateEditText
            )

        dateFields.forEach { id ->

            view.findViewById<TextInputEditText>(
                id
            )
                .setOnClickListener {

                    showDatePicker(
                        it as TextInputEditText
                    )
                }
        }

        val timeFields =
            listOf(
                R.id.accommodationCheckInTimeEditText,
                R.id.accommodationCheckOutTimeEditText,
                R.id.outboundBoardingTimeEditText,
                R.id.outboundDepartureTimeEditText,
                R.id.outboundArrivalTimeEditText,
                R.id.returnBoardingTimeEditText,
                R.id.returnDepartureTimeEditText,
                R.id.returnArrivalTimeEditText
            )

        timeFields.forEach { id ->

            view.findViewById<TextInputEditText>(
                id
            )
                .setOnClickListener {

                    showTimePicker(
                        it as TextInputEditText
                    )
                }
        }
    }

    private fun setupFlyingSwitch(
        view: View
    ) {
        flightDetailsContainer.visibility =
            View.GONE

        flyingSwitch.isChecked =
            false

        flyingSwitch
            .setOnCheckedChangeListener {
                    _,
                    isChecked ->

                flightDetailsContainer.visibility =
                    if (isChecked) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                if (!isChecked) {
                    clearFlightErrors(
                        view
                    )
                }
            }
    }

    private fun showDatePicker(
        target: TextInputEditText
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

    private fun showTimePicker(
        target: TextInputEditText
    ) {
        val calendar =
            Calendar.getInstance()

        TimePickerDialog(
            requireContext(),
            {
                    _,
                    hour,
                    minute ->

                target.setText(
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                    )
                )
            },
            calendar.get(
                Calendar.HOUR_OF_DAY
            ),
            calendar.get(
                Calendar.MINUTE
            ),
            true
        ).show()
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
            !validateMainHolidayDetails(
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

        val accommodationName =
            textOf(
                R.id.accommodationNameEditText
            )

        val accommodationLocation =
            textOf(
                R.id.accommodationLocationEditText
            )

        val accommodationType =
            textOf(
                R.id.accommodationTypeEditText
            )

        val checkInDate =
            textOf(
                R.id.accommodationCheckInDateEditText
            )

        val checkInTime =
            textOf(
                R.id.accommodationCheckInTimeEditText
            )

        val checkOutDate =
            textOf(
                R.id.accommodationCheckOutDateEditText
            )

        val checkOutTime =
            textOf(
                R.id.accommodationCheckOutTimeEditText
            )

        if (
            !validateAccommodation(
                accommodationName,
                accommodationLocation,
                accommodationType,
                checkInDate,
                checkInTime,
                checkOutDate,
                checkOutTime
            )
        ) {
            return
        }

        val accommodation =
            AccommodationDetails(
                name =
                    accommodationName,
                location =
                    accommodationLocation,
                type =
                    accommodationType,
                checkInDate =
                    checkInDate,
                checkInTime =
                    checkInTime,
                checkOutDate =
                    checkOutDate,
                checkOutTime =
                    checkOutTime
            )

        val isFlying =
            flyingSwitch.isChecked

        var outboundFlight:
                FlightDetails? = null

        var returnFlight:
                FlightDetails? = null

        if (isFlying) {
            val outboundAirline =
                textOf(
                    R.id.outboundAirlineEditText
                )

            val outboundRoute =
                textOf(
                    R.id.outboundAirportRouteEditText
                )

            val outboundDate =
                textOf(
                    R.id.outboundFlightDateEditText
                )

            val outboundBoarding =
                textOf(
                    R.id.outboundBoardingTimeEditText
                )

            val outboundDeparture =
                textOf(
                    R.id.outboundDepartureTimeEditText
                )

            val outboundArrival =
                textOf(
                    R.id.outboundArrivalTimeEditText
                )

            val returnAirline =
                textOf(
                    R.id.returnAirlineEditText
                )

            val returnRoute =
                textOf(
                    R.id.returnAirportRouteEditText
                )

            val returnDate =
                textOf(
                    R.id.returnFlightDateEditText
                )

            val returnBoarding =
                textOf(
                    R.id.returnBoardingTimeEditText
                )

            val returnDeparture =
                textOf(
                    R.id.returnDepartureTimeEditText
                )

            val returnArrival =
                textOf(
                    R.id.returnArrivalTimeEditText
                )

            if (
                !validateFlights(
                    outboundAirline,
                    outboundRoute,
                    outboundDate,
                    outboundBoarding,
                    outboundDeparture,
                    outboundArrival,
                    returnAirline,
                    returnRoute,
                    returnDate,
                    returnBoarding,
                    returnDeparture,
                    returnArrival
                )
            ) {
                return
            }

            outboundFlight =
                FlightDetails(
                    airline =
                        outboundAirline,
                    airportRoute =
                        outboundRoute,
                    flightDate =
                        outboundDate,
                    boardingTime =
                        outboundBoarding,
                    departureTime =
                        outboundDeparture,
                    arrivalTime =
                        outboundArrival,
                    terminal =
                        textOf(
                            R.id.outboundTerminalEditText
                        ),
                    boardingGate =
                        textOf(
                            R.id.outboundGateEditText
                        )
                )

            returnFlight =
                FlightDetails(
                    airline =
                        returnAirline,
                    airportRoute =
                        returnRoute,
                    flightDate =
                        returnDate,
                    boardingTime =
                        returnBoarding,
                    departureTime =
                        returnDeparture,
                    arrivalTime =
                        returnArrival,
                    terminal =
                        textOf(
                            R.id.returnTerminalEditText
                        ),
                    boardingGate =
                        textOf(
                            R.id.returnGateEditText
                        )
                )
        }

        val holiday =
            Holiday(
                holidayId =
                    holidayRepository
                        .getNextHolidayId(),
                name =
                    name,
                location =
                    "$city, $country",
                startDate =
                    startDate,
                endDate =
                    endDate,
                tripType =
                    tripType,
                country =
                    country,
                city =
                    city,
                coverImageUri =
                    selectedCoverImageUri
                        ?.toString(),
                accommodation =
                    accommodation,
                isFlying =
                    isFlying,
                outboundFlight =
                    outboundFlight,
                returnFlight =
                    returnFlight
            )

        holidayRepository
            .addHoliday(
                holiday
            )

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

    private fun validateMainHolidayDetails(
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

    private fun validateAccommodation(
        accommodationName: String,
        accommodationLocation: String,
        accommodationType: String,
        checkInDate: String,
        checkInTime: String,
        checkOutDate: String,
        checkOutTime: String
    ): Boolean {

        var valid = true

        valid =
            validateRequiredField(
                R.id.accommodationNameLayout,
                accommodationName
            ) && valid

        valid =
            validateRequiredField(
                R.id.accommodationLocationLayout,
                accommodationLocation
            ) && valid

        valid =
            validateRequiredField(
                R.id.accommodationTypeLayout,
                accommodationType
            ) && valid

        valid =
            validateRequiredField(
                R.id.accommodationCheckInDateLayout,
                checkInDate
            ) && valid

        valid =
            validateRequiredField(
                R.id.accommodationCheckInTimeLayout,
                checkInTime
            ) && valid

        valid =
            validateRequiredField(
                R.id.accommodationCheckOutDateLayout,
                checkOutDate
            ) && valid

        valid =
            validateRequiredField(
                R.id.accommodationCheckOutTimeLayout,
                checkOutTime
            ) && valid

        if (
            checkInDate.isNotBlank() &&
            checkOutDate.isNotBlank()
        ) {
            try {
                val parsedCheckIn =
                    dateFormat.parse(
                        checkInDate
                    )

                val parsedCheckOut =
                    dateFormat.parse(
                        checkOutDate
                    )

                if (
                    parsedCheckIn != null &&
                    parsedCheckOut != null &&
                    parsedCheckOut.before(
                        parsedCheckIn
                    )
                ) {
                    requireView()
                        .findViewById<TextInputLayout>(
                            R.id
                                .accommodationCheckOutDateLayout
                        )
                        .error =
                        getString(
                            R.string
                                .holiday_invalid_accommodation_dates
                        )

                    valid = false
                }
            } catch (
                exception: Exception
            ) {
                Log.e(
                    "RoamlyHoliday",
                    "Accommodation date validation failed",
                    exception
                )

                valid = false
            }
        }

        return valid
    }

    private fun validateFlights(
        outboundAirline: String,
        outboundRoute: String,
        outboundDate: String,
        outboundBoarding: String,
        outboundDeparture: String,
        outboundArrival: String,
        returnAirline: String,
        returnRoute: String,
        returnDate: String,
        returnBoarding: String,
        returnDeparture: String,
        returnArrival: String
    ): Boolean {

        var valid = true

        valid =
            validateRequiredField(
                R.id.outboundAirlineLayout,
                outboundAirline
            ) && valid

        valid =
            validateRequiredField(
                R.id.outboundAirportRouteLayout,
                outboundRoute
            ) && valid

        valid =
            validateRequiredField(
                R.id.outboundFlightDateLayout,
                outboundDate
            ) && valid

        valid =
            validateRequiredField(
                R.id.outboundBoardingTimeLayout,
                outboundBoarding
            ) && valid

        valid =
            validateRequiredField(
                R.id.outboundDepartureTimeLayout,
                outboundDeparture
            ) && valid

        valid =
            validateRequiredField(
                R.id.outboundArrivalTimeLayout,
                outboundArrival
            ) && valid

        valid =
            validateRequiredField(
                R.id.returnAirlineLayout,
                returnAirline
            ) && valid

        valid =
            validateRequiredField(
                R.id.returnAirportRouteLayout,
                returnRoute
            ) && valid

        valid =
            validateRequiredField(
                R.id.returnFlightDateLayout,
                returnDate
            ) && valid

        valid =
            validateRequiredField(
                R.id.returnBoardingTimeLayout,
                returnBoarding
            ) && valid

        valid =
            validateRequiredField(
                R.id.returnDepartureTimeLayout,
                returnDeparture
            ) && valid

        valid =
            validateRequiredField(
                R.id.returnArrivalTimeLayout,
                returnArrival
            ) && valid

        if (
            outboundDate.isNotBlank() &&
            returnDate.isNotBlank()
        ) {
            try {
                val parsedOutbound =
                    dateFormat.parse(
                        outboundDate
                    )

                val parsedReturn =
                    dateFormat.parse(
                        returnDate
                    )

                if (
                    parsedOutbound != null &&
                    parsedReturn != null &&
                    parsedReturn.before(
                        parsedOutbound
                    )
                ) {
                    requireView()
                        .findViewById<TextInputLayout>(
                            R.id.returnFlightDateLayout
                        )
                        .error =
                        getString(
                            R.string
                                .holiday_return_flight_before_departure
                        )

                    valid = false
                }
            } catch (
                exception: Exception
            ) {
                Log.e(
                    "RoamlyHoliday",
                    "Flight date validation failed",
                    exception
                )

                valid = false
            }
        }

        return valid
    }

    private fun validateRequiredField(
        layoutId: Int,
        value: String
    ): Boolean {

        if (value.isNotBlank()) {
            return true
        }

        requireView()
            .findViewById<TextInputLayout>(
                layoutId
            )
            .error =
            getString(
                R.string.field_required
            )

        return false
    }

    private fun textOf(
        id: Int
    ): String {

        return requireView()
            .findViewById<TextInputEditText>(
                id
            )
            .text
            ?.toString()
            ?.trim()
            .orEmpty()
    }

    private fun clearErrors() {
        nameLayout.error = null
        tripTypeLayout.error = null
        countryLayout.error = null
        cityLayout.error = null
        startDateLayout.error = null
        endDateLayout.error = null

        val additionalLayouts =
            listOf(
                R.id.accommodationNameLayout,
                R.id.accommodationLocationLayout,
                R.id.accommodationTypeLayout,
                R.id.accommodationCheckInDateLayout,
                R.id.accommodationCheckInTimeLayout,
                R.id.accommodationCheckOutDateLayout,
                R.id.accommodationCheckOutTimeLayout,
                R.id.outboundAirlineLayout,
                R.id.outboundAirportRouteLayout,
                R.id.outboundFlightDateLayout,
                R.id.outboundBoardingTimeLayout,
                R.id.outboundDepartureTimeLayout,
                R.id.outboundArrivalTimeLayout,
                R.id.returnAirlineLayout,
                R.id.returnAirportRouteLayout,
                R.id.returnFlightDateLayout,
                R.id.returnBoardingTimeLayout,
                R.id.returnDepartureTimeLayout,
                R.id.returnArrivalTimeLayout
            )

        additionalLayouts.forEach { id ->

            requireView()
                .findViewById<TextInputLayout>(
                    id
                )
                .error = null
        }
    }

    private fun clearFlightErrors(
        view: View
    ) {
        val flightLayouts =
            listOf(
                R.id.outboundAirlineLayout,
                R.id.outboundAirportRouteLayout,
                R.id.outboundFlightDateLayout,
                R.id.outboundBoardingTimeLayout,
                R.id.outboundDepartureTimeLayout,
                R.id.outboundArrivalTimeLayout,
                R.id.returnAirlineLayout,
                R.id.returnAirportRouteLayout,
                R.id.returnFlightDateLayout,
                R.id.returnBoardingTimeLayout,
                R.id.returnDepartureTimeLayout,
                R.id.returnArrivalTimeLayout
            )

        flightLayouts.forEach { id ->

            view.findViewById<TextInputLayout>(
                id
            )
                .error = null
        }
    }
}