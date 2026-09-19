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

class EditHolidayFragment :
    Fragment(R.layout.fragment_edit_holiday) {

    private lateinit var holidayRepository:
            HolidayRepository

    private lateinit var currentHoliday:
            Holiday

    private var holidayId:
            Int = -1

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

    private lateinit var flyingSwitch:
            SwitchMaterial

    private lateinit var flightDetailsContainer:
            View

    private lateinit var saveChangesButton:
            MaterialButton

    private lateinit var changeCoverImageButton:
            MaterialButton

    private var selectedImageUri:
            Uri? = null

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
            } catch (
                exception: SecurityException
            ) {
                Log.w(
                    "RoamlyHoliday",
                    "Could not persist edited cover image permission",
                    exception
                )
            }

            selectedImageUri =
                uri

            coverImageView
                .setImageURI(
                    uri
                )
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

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

        dateFormat.isLenient =
            false

        holidayRepository =
            HolidayRepository(
                requireContext()
                    .applicationContext
            )

        bindViews(
            view
        )

        setupTripTypes()

        setupDateAndTimePickers(
            view
        )

        setupFlyingSwitch()

        setupButtons()

        loadHoliday()
    }

    private fun bindViews(
        view: View
    ) {
        nameLayout =
            view.findViewById(
                R.id.editHolidayNameLayout
            )

        tripTypeLayout =
            view.findViewById(
                R.id.editTripTypeLayout
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

        flyingSwitch =
            view.findViewById(
                R.id.editFlyingSwitch
            )

        flightDetailsContainer =
            view.findViewById(
                R.id.editFlightDetailsContainer
            )

        saveChangesButton =
            view.findViewById(
                R.id.saveHolidayChangesButton
            )

        changeCoverImageButton =
            view.findViewById(
                R.id.changeCoverImageButton
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
            .setAdapter(
                adapter
            )
    }

    private fun setupDateAndTimePickers(
        view: View
    ) {
        val dateFields =
            listOf(
                R.id.editStartDateEditText,
                R.id.editEndDateEditText,
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

    private fun setupFlyingSwitch() {
        flyingSwitch
            .setOnCheckedChangeListener {
                    _,
                    isChecked ->

                flightDetailsContainer
                    .visibility =
                    if (isChecked) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                if (!isChecked) {
                    clearFlightErrors()
                }
            }
    }

    private fun setupButtons() {
        changeCoverImageButton
            .setOnClickListener {

                imagePicker.launch(
                    arrayOf(
                        "image/*"
                    )
                )
            }

        saveChangesButton
            .setOnClickListener {

                saveChanges()
            }
    }

    private fun loadHoliday() {
        val holiday =
            holidayRepository
                .getHolidayById(
                    holidayId
                )

        if (holiday == null) {
            (
                    requireActivity()
                            as MainActivity
                    )
                .showHome()

            return
        }

        currentHoliday =
            holiday

        nameEditText
            .setText(
                holiday.name
            )

        tripTypeAutoComplete
            .setText(
                holiday.tripType,
                false
            )

        countryEditText
            .setText(
                holiday.country
            )

        cityEditText
            .setText(
                holiday.city
            )

        startDateEditText
            .setText(
                holiday.startDate
            )

        endDateEditText
            .setText(
                holiday.endDate
            )

        loadAccommodation(
            holiday
        )

        loadFlightDetails(
            holiday
        )

        loadCoverImage(
            holiday
        )
    }

    private fun loadAccommodation(
        holiday: Holiday
    ) {
        val accommodation =
            holiday.accommodation

        if (accommodation == null) {
            return
        }

        setText(
            R.id.accommodationNameEditText,
            accommodation.name
        )

        setText(
            R.id.accommodationLocationEditText,
            accommodation.location
        )

        setText(
            R.id.accommodationTypeEditText,
            accommodation.type
        )

        setText(
            R.id.accommodationCheckInDateEditText,
            accommodation.checkInDate
        )

        setText(
            R.id.accommodationCheckInTimeEditText,
            accommodation.checkInTime
        )

        setText(
            R.id.accommodationCheckOutDateEditText,
            accommodation.checkOutDate
        )

        setText(
            R.id.accommodationCheckOutTimeEditText,
            accommodation.checkOutTime
        )
    }

    private fun loadFlightDetails(
        holiday: Holiday
    ) {
        flyingSwitch.isChecked =
            holiday.isFlying

        flightDetailsContainer.visibility =
            if (holiday.isFlying) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (!holiday.isFlying) {
            return
        }

        holiday.outboundFlight
            ?.let { flight ->

                setText(
                    R.id.outboundAirlineEditText,
                    flight.airline
                )

                setText(
                    R.id.outboundAirportRouteEditText,
                    flight.airportRoute
                )

                setText(
                    R.id.outboundFlightDateEditText,
                    flight.flightDate
                )

                setText(
                    R.id.outboundBoardingTimeEditText,
                    flight.boardingTime
                )

                setText(
                    R.id.outboundDepartureTimeEditText,
                    flight.departureTime
                )

                setText(
                    R.id.outboundArrivalTimeEditText,
                    flight.arrivalTime
                )

                setText(
                    R.id.outboundTerminalEditText,
                    flight.terminal
                )

                setText(
                    R.id.outboundGateEditText,
                    flight.boardingGate
                )
            }

        holiday.returnFlight
            ?.let { flight ->

                setText(
                    R.id.returnAirlineEditText,
                    flight.airline
                )

                setText(
                    R.id.returnAirportRouteEditText,
                    flight.airportRoute
                )

                setText(
                    R.id.returnFlightDateEditText,
                    flight.flightDate
                )

                setText(
                    R.id.returnBoardingTimeEditText,
                    flight.boardingTime
                )

                setText(
                    R.id.returnDepartureTimeEditText,
                    flight.departureTime
                )

                setText(
                    R.id.returnArrivalTimeEditText,
                    flight.arrivalTime
                )

                setText(
                    R.id.returnTerminalEditText,
                    flight.terminal
                )

                setText(
                    R.id.returnGateEditText,
                    flight.boardingGate
                )
            }
    }

    private fun loadCoverImage(
        holiday: Holiday
    ) {
        if (
            holiday.coverImageUri
                .isNullOrBlank()
        ) {
            coverImageView
                .setImageResource(
                    R.drawable.ic_holiday_placeholder
                )

            return
        }

        try {
            coverImageView
                .setImageURI(
                    Uri.parse(
                        holiday.coverImageUri
                    )
                )
        } catch (
            exception: Exception
        ) {
            Log.e(
                "RoamlyHoliday",
                "Could not load holiday cover image",
                exception
            )

            coverImageView
                .setImageResource(
                    R.drawable.ic_holiday_placeholder
                )
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

        val imageUri =
            selectedImageUri
                ?.toString()
                ?: currentHoliday
                    .coverImageUri

        val updatedHoliday =
            currentHoliday.copy(
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
                    imageUri,
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
            .updateHoliday(
                updatedHoliday
            )

        Log.i(
            "RoamlyHoliday",
            "Holiday updated: ${updatedHoliday.holidayId}"
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

    private fun validateMainHolidayDetails(
        name: String,
        tripType: String,
        country: String,
        city: String,
        startDate: String,
        endDate: String
    ): Boolean {

        var valid =
            true

        if (name.isBlank()) {
            nameLayout.error =
                getString(
                    R.string.field_required
                )

            valid =
                false
        }

        if (tripType.isBlank()) {
            tripTypeLayout.error =
                getString(
                    R.string.select_trip_type
                )

            valid =
                false
        }

        if (country.isBlank()) {
            countryLayout.error =
                getString(
                    R.string.field_required
                )

            valid =
                false
        }

        if (city.isBlank()) {
            cityLayout.error =
                getString(
                    R.string.field_required
                )

            valid =
                false
        }

        if (startDate.isBlank()) {
            startDateLayout.error =
                getString(
                    R.string.select_start_date
                )

            valid =
                false
        }

        if (endDate.isBlank()) {
            endDateLayout.error =
                getString(
                    R.string.select_end_date
                )

            valid =
                false
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

                    valid =
                        false
                }
            } catch (
                exception: Exception
            ) {
                Log.e(
                    "RoamlyHoliday",
                    "Holiday date validation failed",
                    exception
                )

                valid =
                    false
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

        var valid =
            true

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

                    valid =
                        false
                }
            } catch (
                exception: Exception
            ) {
                Log.e(
                    "RoamlyHoliday",
                    "Accommodation date validation failed",
                    exception
                )

                valid =
                    false
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

        var valid =
            true

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

                    valid =
                        false
                }
            } catch (
                exception: Exception
            ) {
                Log.e(
                    "RoamlyHoliday",
                    "Flight date validation failed",
                    exception
                )

                valid =
                    false
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
        ).show()
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

    private fun setText(
        id: Int,
        value: String
    ) {
        requireView()
            .findViewById<TextInputEditText>(
                id
            )
            .setText(
                value
            )
    }

    private fun clearErrors() {
        nameLayout.error =
            null

        tripTypeLayout.error =
            null

        countryLayout.error =
            null

        cityLayout.error =
            null

        startDateLayout.error =
            null

        endDateLayout.error =
            null

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
                .error =
                null
        }
    }

    private fun clearFlightErrors() {
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

            requireView()
                .findViewById<TextInputLayout>(
                    id
                )
                .error =
                null
        }
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