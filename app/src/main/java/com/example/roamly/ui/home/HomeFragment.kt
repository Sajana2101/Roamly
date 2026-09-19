package com.example.roamly.ui.home

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.repository.HolidayRepository
import com.google.android.material.button.MaterialButton

class HomeFragment :
    Fragment(R.layout.fragment_home) {

    private lateinit var holidayRepository:
            HolidayRepository

    private lateinit var holidayAdapter:
            HolidayAdapter

    private lateinit var holidaysRecyclerView:
            RecyclerView

    private lateinit var emptyHolidayState:
            LinearLayout

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

        holidayAdapter =
            HolidayAdapter()

        holidaysRecyclerView =
            view.findViewById(
                R.id.holidaysRecyclerView
            )

        emptyHolidayState =
            view.findViewById(
                R.id.emptyHolidayState
            )

        val planHolidayButton =
            view.findViewById<MaterialButton>(
                R.id.planHolidayButton
            )

        holidaysRecyclerView.apply {
            layoutManager =
                LinearLayoutManager(
                    requireContext()
                )

            adapter =
                holidayAdapter
        }

        planHolidayButton
            .setOnClickListener {
                (
                        requireActivity()
                                as MainActivity
                        )
                    .showAddHoliday()
            }

        loadHolidays()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::holidayRepository
                .isInitialized
        ) {
            loadHolidays()
        }
    }

    private fun loadHolidays() {
        val holidays =
            holidayRepository
                .getHolidays()
                .sortedBy {
                    it.startDate
                }

        holidayAdapter
            .submitList(holidays)

        if (holidays.isEmpty()) {
            holidaysRecyclerView
                .visibility =
                View.GONE

            emptyHolidayState
                .visibility =
                View.VISIBLE
        } else {
            holidaysRecyclerView
                .visibility =
                View.VISIBLE

            emptyHolidayState
                .visibility =
                View.GONE
        }
    }
}