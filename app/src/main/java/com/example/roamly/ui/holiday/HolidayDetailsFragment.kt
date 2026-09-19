package com.example.roamly.ui.holiday

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.roamly.MainActivity
import com.example.roamly.R
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

    private var holidayId: Int = -1

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

        editButton.setOnClickListener {
            (
                    requireActivity()
                            as MainActivity
                    )
                .showEditHoliday(
                    holidayId
                )
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        backButton.setOnClickListener {
            (
                    requireActivity()
                            as MainActivity
                    )
                .showHome()
        }

        loadHoliday(view)
    }

    override fun onResume() {
        super.onResume()

        view?.let {
            loadHoliday(it)
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
                ?: run {
                    (
                            requireActivity()
                                    as MainActivity
                            )
                        .showHome()

                    return
                }

        bindHoliday(
            view,
            holiday
        )
    }

    private fun bindHoliday(
        view: View,
        holiday: Holiday
    ) {
        val coverImage =
            view.findViewById<ImageView>(
                R.id.detailsCoverImageView
            )

        val titleText =
            view.findViewById<TextView>(
                R.id.detailsHolidayTitleTextView
            )

        val statusText =
            view.findViewById<TextView>(
                R.id.detailsStatusTextView
            )

        val countdownText =
            view.findViewById<TextView>(
                R.id.detailsCountdownTextView
            )

        val tripTypeText =
            view.findViewById<TextView>(
                R.id.detailsTripTypeTextView
            )

        val locationText =
            view.findViewById<TextView>(
                R.id.detailsLocationTextView
            )

        val datesText =
            view.findViewById<TextView>(
                R.id.detailsDatesTextView
            )

        titleText.text =
            holiday.name

        tripTypeText.text =
            "✦ ${holiday.tripType}"

        locationText.text =
            "📍 ${holiday.location}"

        datesText.text =
            "▣ ${formatDate(holiday.startDate)} - " +
                    formatDate(holiday.endDate)

        loadCoverImage(
            coverImage,
            holiday
        )

        updateStatus(
            holiday,
            statusText,
            countdownText
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
                Uri.parse(imageUri)
            )
        } catch (_: Exception) {
            imageView.setImageResource(
                R.drawable.ic_holiday_placeholder
            )
        }
    }

    private fun updateStatus(
        holiday: Holiday,
        statusText: TextView,
        countdownText: TextView
    ) {
        try {
            val format =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val start =
                format.parse(
                    holiday.startDate
                )

            val end =
                format.parse(
                    holiday.endDate
                )

            if (
                start == null ||
                end == null
            ) {
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

            val current =
                today.time

            when {
                current.before(start) -> {
                    statusText.text =
                        getString(
                            R.string.holiday_upcoming
                        )

                    val days =
                        TimeUnit.MILLISECONDS
                            .toDays(
                                start.time -
                                        current.time
                            )

                    countdownText.text =
                        "✈  " +
                                getString(
                                    R.string.holiday_days_to_go,
                                    days
                                )
                }

                current.after(end) -> {
                    statusText.text =
                        getString(
                            R.string.holiday_completed
                        )

                    countdownText.text =
                        getString(
                            R.string.holiday_finished
                        )
                }

                else -> {
                    statusText.text =
                        getString(
                            R.string.holiday_in_progress
                        )

                    countdownText.text =
                        getString(
                            R.string.holiday_started
                        )
                }
            }
        } catch (_: Exception) {
            statusText.text = ""
            countdownText.text = ""
        }
    }

    private fun formatDate(
        date: String
    ): String {
        return try {
            val input =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val output =
                SimpleDateFormat(
                    "d MMM yyyy",
                    Locale.getDefault()
                )

            val parsed =
                input.parse(date)

            if (parsed != null) {
                output.format(parsed)
            } else {
                date
            }
        } catch (_: Exception) {
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
            ) { _, _ ->

                holidayRepository
                    .deleteHoliday(
                        holidayId
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
            .show()
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