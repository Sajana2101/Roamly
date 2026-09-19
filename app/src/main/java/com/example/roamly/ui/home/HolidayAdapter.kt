package com.example.roamly.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        private val titleTextView =
            itemView.findViewById<TextView>(
                R.id.holidayTitleTextView
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
                holiday.title

            tripTypeTextView.text =
                holiday.tripType

            locationTextView.text =
                context.getString(
                    R.string.holiday_location_format,
                    holiday.city,
                    holiday.country
                )

            datesTextView.text =
                context.getString(
                    R.string.holiday_date_format,
                    holiday.startDate,
                    holiday.endDate
                )

            countdownTextView.text =
                getCountdownText(
                    holiday
                )
        }

        private fun getCountdownText(
            holiday: Holiday
        ): String {
            val context =
                itemView.context

            return try {
                val dateFormat =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    )

                dateFormat.isLenient = false

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
                    return ""
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
                    currentDate.before(startDate) -> {
                        val difference =
                            startDate.time -
                                    currentDate.time

                        val days =
                            TimeUnit.MILLISECONDS
                                .toDays(difference)

                        context.getString(
                            R.string
                                .holiday_countdown_days,
                            days
                        )
                    }

                    currentDate.after(endDate) -> {
                        context.getString(
                            R.string
                                .holiday_finished
                        )
                    }

                    else -> {
                        context.getString(
                            R.string
                                .holiday_started
                        )
                    }
                }
            } catch (_: Exception) {
                ""
            }
        }
    }
}