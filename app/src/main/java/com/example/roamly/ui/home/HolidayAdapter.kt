package com.example.roamly.ui.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.Holiday
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class HolidayAdapter :
    RecyclerView.Adapter<HolidayAdapter.HolidayViewHolder>() {

    private val holidays =
        mutableListOf<Holiday>()

    fun submitList(
        newHolidays: List<Holiday>
    ) {
        holidays.clear()
        holidays.addAll(newHolidays)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HolidayViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_holiday,
                    parent,
                    false
                )

        return HolidayViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HolidayViewHolder,
        position: Int
    ) {
        holder.bind(
            holidays[position]
        )
    }

    override fun getItemCount(): Int {
        return holidays.size
    }

    class HolidayViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val coverImageView =
            itemView.findViewById<ImageView>(
                R.id.holidayCoverImageView
            )

        private val titleTextView =
            itemView.findViewById<TextView>(
                R.id.holidayTitleTextView
            )

        private val statusTextView =
            itemView.findViewById<TextView>(
                R.id.holidayStatusTextView
            )

        private val tripTypeTextView =
            itemView.findViewById<TextView>(
                R.id.holidayTripTypeTextView
            )

        private val locationTextView =
            itemView.findViewById<TextView>(
                R.id.holidayLocationTextView
            )

        private val datesTextView =
            itemView.findViewById<TextView>(
                R.id.holidayDatesTextView
            )

        private val countdownTextView =
            itemView.findViewById<TextView>(
                R.id.holidayCountdownTextView
            )

        fun bind(
            holiday: Holiday
        ) {
            val context =
                itemView.context

            titleTextView.text =
                holiday.name

            tripTypeTextView.text =
                "✦ ${holiday.tripType}"

            locationTextView.text =
                "📍 ${holiday.location}"

            datesTextView.text =
                "▣ ${formatDisplayDate(holiday.startDate)} - " +
                        formatDisplayDate(holiday.endDate)

            loadCoverImage(holiday)

            updateStatusAndCountdown(
                holiday
            )
        }

        private fun loadCoverImage(
            holiday: Holiday
        ) {
            val imageUri =
                holiday.coverImageUri

            if (imageUri.isNullOrBlank()) {
                coverImageView.setImageResource(
                    R.drawable.ic_holiday_placeholder
                )

                return
            }

            try {
                coverImageView.setImageURI(
                    Uri.parse(imageUri)
                )
            } catch (_: Exception) {
                coverImageView.setImageResource(
                    R.drawable.ic_holiday_placeholder
                )
            }
        }

        private fun updateStatusAndCountdown(
            holiday: Holiday
        ) {
            val context =
                itemView.context

            try {
                val format =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    )

                format.isLenient = false

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
                    statusTextView.text = ""
                    countdownTextView.text = ""

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
                        statusTextView.text =
                            context.getString(
                                R.string.holiday_upcoming
                            )

                        val difference =
                            start.time -
                                    current.time

                        val days =
                            TimeUnit.MILLISECONDS
                                .toDays(difference)

                        countdownTextView.text =
                            "✈  " +
                                    context.getString(
                                        R.string
                                            .holiday_days_to_go,
                                        days
                                    )
                    }

                    current.after(end) -> {
                        statusTextView.text =
                            context.getString(
                                R.string.holiday_completed
                            )

                        countdownTextView.text =
                            context.getString(
                                R.string.holiday_finished
                            )
                    }

                    else -> {
                        statusTextView.text =
                            context.getString(
                                R.string.holiday_in_progress
                            )

                        countdownTextView.text =
                            context.getString(
                                R.string.holiday_started
                            )
                    }
                }
            } catch (_: Exception) {
                statusTextView.text = ""
                countdownTextView.text = ""
            }
        }

        private fun formatDisplayDate(
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
                        "d MMM",
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
    }
}