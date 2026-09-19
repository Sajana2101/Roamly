package com.example.roamly.ui.holiday

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.model.FlightDetails
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.repository.HolidayRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class HolidayDetailsFragment :
    Fragment(R.layout.fragment_holiday_details) {

    private lateinit var holidayRepository:
            HolidayRepository

    private var holidayId:
            Int = -1

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

        holidayRepository =
            HolidayRepository(
                requireContext()
                    .applicationContext
            )

        setupButtons(
            view
        )

        loadHoliday(
            view
        )
    }

    override fun onResume() {
        super.onResume()

        view?.let {
            loadHoliday(
                it
            )
        }
    }

    private fun setupButtons(
        view: View
    ) {
        val editButton =
            view.findViewById<MaterialButton>(
                R.id.editHolidayButton
            )

        val deleteButton =
            view.findViewById<MaterialButton>(
                R.id.deleteHolidayButton
            )

        val backButton =
            view.findViewById<MaterialButton>(
                R.id.backToHolidaysButton
            )

        editButton
            .setOnClickListener {

                (
                        requireActivity()
                                as MainActivity
                        )
                    .showEditHoliday(
                        holidayId
                    )
            }

        deleteButton
            .setOnClickListener {

                showDeleteConfirmation()
            }

        backButton
            .setOnClickListener {

                (
                        requireActivity()
                                as MainActivity
                        )
                    .showHome()
            }
    }

    private fun loadHoliday(
        view: View
    ) {
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

        bindMainHolidayDetails(
            view,
            holiday
        )

        bindAccommodation(
            view,
            holiday
        )

        bindFlights(
            view,
            holiday
        )
    }

    private fun bindMainHolidayDetails(
        view: View,
        holiday: Holiday
    ) {
        val coverImageView =
            view.findViewById<ImageView>(
                R.id.detailsCoverImageView
            )

        val titleTextView =
            view.findViewById<TextView>(
                R.id.detailsHolidayTitleTextView
            )

        val statusTextView =
            view.findViewById<TextView>(
                R.id.detailsStatusTextView
            )

        val countdownTextView =
            view.findViewById<TextView>(
                R.id.detailsCountdownTextView
            )

        val tripTypeTextView =
            view.findViewById<TextView>(
                R.id.detailsTripTypeTextView
            )

        val locationTextView =
            view.findViewById<TextView>(
                R.id.detailsLocationTextView
            )

        val datesTextView =
            view.findViewById<TextView>(
                R.id.detailsDatesTextView
            )

        titleTextView.text =
            holiday.name

        tripTypeTextView.text =
            "✦ ${holiday.tripType}"

        locationTextView.text =
            "📍 ${holiday.location}"

        datesTextView.text =
            "▣ ${formatDate(holiday.startDate)} - " +
                    formatDate(
                        holiday.endDate
                    )

        loadCoverImage(
            coverImageView,
            holiday
        )

        updateStatus(
            holiday,
            statusTextView,
            countdownTextView
        )
    }

    private fun bindAccommodation(
        view: View,
        holiday: Holiday
    ) {
        val container =
            view.findViewById<LinearLayout>(
                R.id.detailsAccommodationContainer
            )

        val accommodation =
            holiday.accommodation

        if (accommodation == null) {

            container.visibility =
                View.GONE

            return
        }

        container.visibility =
            View.VISIBLE

        view.findViewById<TextView>(
            R.id.detailsAccommodationNameTextView
        ).text =
            accommodation.name

        view.findViewById<TextView>(
            R.id.detailsAccommodationTypeTextView
        ).text =
            getString(
                R.string.holiday_view_accommodation_type,
                accommodation.type
            )

        view.findViewById<TextView>(
            R.id.detailsAccommodationLocationTextView
        ).text =
            getString(
                R.string.holiday_view_accommodation_location,
                accommodation.location
            )

        view.findViewById<TextView>(
            R.id.detailsCheckInTextView
        ).text =
            getString(
                R.string.holiday_view_check_in,
                formatDate(
                    accommodation.checkInDate
                ),
                accommodation.checkInTime
            )

        view.findViewById<TextView>(
            R.id.detailsCheckOutTextView
        ).text =
            getString(
                R.string.holiday_view_check_out,
                formatDate(
                    accommodation.checkOutDate
                ),
                accommodation.checkOutTime
            )
    }

    private fun bindFlights(
        view: View,
        holiday: Holiday
    ) {
        val flightContainer =
            view.findViewById<LinearLayout>(
                R.id.detailsFlightContainer
            )

        if (!holiday.isFlying) {

            flightContainer.visibility =
                View.GONE

            return
        }

        flightContainer.visibility =
            View.VISIBLE

        bindOutboundFlight(
            view,
            holiday.outboundFlight
        )

        bindReturnFlight(
            view,
            holiday.returnFlight
        )
    }

    private fun bindOutboundFlight(
        view: View,
        flight: FlightDetails?
    ) {
        val card =
            view.findViewById<View>(
                R.id.detailsOutboundFlightCard
            )

        if (flight == null) {

            card.visibility =
                View.GONE

            return
        }

        card.visibility =
            View.VISIBLE

        view.findViewById<TextView>(
            R.id.detailsOutboundAirlineTextView
        ).text =
            flight.airline

        view.findViewById<TextView>(
            R.id.detailsOutboundRouteTextView
        ).text =
            getString(
                R.string.holiday_view_route,
                flight.airportRoute
            )

        view.findViewById<TextView>(
            R.id.detailsOutboundDateTextView
        ).text =
            getString(
                R.string.holiday_view_flight_date,
                formatDate(
                    flight.flightDate
                )
            )

        view.findViewById<TextView>(
            R.id.detailsOutboundBoardingTextView
        ).text =
            getString(
                R.string.holiday_view_boarding_time,
                flight.boardingTime
            )

        view.findViewById<TextView>(
            R.id.detailsOutboundDepartureTextView
        ).text =
            getString(
                R.string.holiday_view_departure_time,
                flight.departureTime
            )

        view.findViewById<TextView>(
            R.id.detailsOutboundArrivalTextView
        ).text =
            getString(
                R.string.holiday_view_arrival_time,
                flight.arrivalTime
            )

        setOptionalFlightText(
            view.findViewById(
                R.id.detailsOutboundTerminalTextView
            ),
            R.string.holiday_view_terminal,
            flight.terminal
        )

        setOptionalFlightText(
            view.findViewById(
                R.id.detailsOutboundGateTextView
            ),
            R.string.holiday_view_gate,
            flight.boardingGate
        )
    }

    private fun bindReturnFlight(
        view: View,
        flight: FlightDetails?
    ) {
        val card =
            view.findViewById<View>(
                R.id.detailsReturnFlightCard
            )

        if (flight == null) {

            card.visibility =
                View.GONE

            return
        }

        card.visibility =
            View.VISIBLE

        view.findViewById<TextView>(
            R.id.detailsReturnAirlineTextView
        ).text =
            flight.airline

        view.findViewById<TextView>(
            R.id.detailsReturnRouteTextView
        ).text =
            getString(
                R.string.holiday_view_route,
                flight.airportRoute
            )

        view.findViewById<TextView>(
            R.id.detailsReturnDateTextView
        ).text =
            getString(
                R.string.holiday_view_flight_date,
                formatDate(
                    flight.flightDate
                )
            )

        view.findViewById<TextView>(
            R.id.detailsReturnBoardingTextView
        ).text =
            getString(
                R.string.holiday_view_boarding_time,
                flight.boardingTime
            )

        view.findViewById<TextView>(
            R.id.detailsReturnDepartureTextView
        ).text =
            getString(
                R.string.holiday_view_departure_time,
                flight.departureTime
            )

        view.findViewById<TextView>(
            R.id.detailsReturnArrivalTextView
        ).text =
            getString(
                R.string.holiday_view_arrival_time,
                flight.arrivalTime
            )

        setOptionalFlightText(
            view.findViewById(
                R.id.detailsReturnTerminalTextView
            ),
            R.string.holiday_view_terminal,
            flight.terminal
        )

        setOptionalFlightText(
            view.findViewById(
                R.id.detailsReturnGateTextView
            ),
            R.string.holiday_view_gate,
            flight.boardingGate
        )
    }

    private fun setOptionalFlightText(
        textView: TextView,
        stringResource: Int,
        value: String
    ) {
        if (value.isBlank()) {

            textView.visibility =
                View.GONE

            return
        }

        textView.visibility =
            View.VISIBLE

        textView.text =
            getString(
                stringResource,
                value
            )
    }

    private fun loadCoverImage(
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

            Log.e(
                "RoamlyHoliday",
                "Could not load holiday cover image",
                exception
            )

            imageView.setImageResource(
                R.drawable.ic_holiday_placeholder
            )
        }
    }

    private fun updateStatus(
        holiday: Holiday,
        statusTextView: TextView,
        countdownTextView: TextView
    ) {
        try {

            val dateFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            dateFormat.isLenient =
                false

            val startDate =
                dateFormat.parse(
                    holiday.startDate
                )

            val endDate =
                dateFormat.parse(
                    holiday.endDate
                )

            if (
                startDate == null ||
                endDate == null
            ) {

                statusTextView.text =
                    ""

                countdownTextView.text =
                    ""

                return
            }

            val today =
                Calendar.getInstance()

            today.set(
                Calendar.HOUR_OF_DAY,
                0
            )

            today.set(
                Calendar.MINUTE,
                0
            )

            today.set(
                Calendar.SECOND,
                0
            )

            today.set(
                Calendar.MILLISECOND,
                0
            )

            val currentDate =
                today.time

            when {

                currentDate.before(
                    startDate
                ) -> {

                    statusTextView.text =
                        getString(
                            R.string.holiday_upcoming
                        )

                    val difference =
                        startDate.time -
                                currentDate.time

                    val days =
                        TimeUnit.MILLISECONDS
                            .toDays(
                                difference
                            )

                    countdownTextView.text =
                        "✈  " +
                                getString(
                                    R.string.holiday_days_to_go,
                                    days
                                )
                }

                currentDate.after(
                    endDate
                ) -> {

                    statusTextView.text =
                        getString(
                            R.string.holiday_completed
                        )

                    countdownTextView.text =
                        getString(
                            R.string.holiday_finished
                        )
                }

                else -> {

                    statusTextView.text =
                        getString(
                            R.string.holiday_in_progress
                        )

                    countdownTextView.text =
                        getString(
                            R.string.holiday_started
                        )
                }
            }

        } catch (
            exception: Exception
        ) {

            Log.e(
                "RoamlyHoliday",
                "Could not calculate holiday status",
                exception
            )

            statusTextView.text =
                ""

            countdownTextView.text =
                ""
        }
    }

    private fun formatDate(
        date: String
    ): String {

        return try {

            val inputFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            inputFormat.isLenient =
                false

            val outputFormat =
                SimpleDateFormat(
                    "d MMM yyyy",
                    Locale.getDefault()
                )

            val parsedDate =
                inputFormat.parse(
                    date
                )

            if (parsedDate != null) {

                outputFormat.format(
                    parsedDate
                )

            } else {

                date
            }

        } catch (
            exception: Exception
        ) {

            date
        }
    }

    private fun showDeleteConfirmation() {

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                R.string.delete_holiday_title
            )
            .setMessage(
                R.string.delete_holiday_message
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete
            ) {
                    _,
                    _ ->

                deleteHoliday()
            }
            .show()
    }

    private fun deleteHoliday() {

        holidayRepository
            .deleteHoliday(
                holidayId
            )

        Log.i(
            "RoamlyHoliday",
            "Holiday deleted: $holidayId"
        )

        Toast.makeText(
            requireContext(),
            getString(
                R.string.holiday_deleted
            ),
            Toast.LENGTH_SHORT
        ).show()

        (
                requireActivity()
                        as MainActivity
                )
            .showHome()
    }

    companion object {

        private const val ARG_HOLIDAY_ID =
            "holiday_id"

        fun newInstance(
            holidayId: Int
        ): HolidayDetailsFragment {

            return HolidayDetailsFragment()
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