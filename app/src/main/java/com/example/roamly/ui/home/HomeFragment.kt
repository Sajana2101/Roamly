
package com.example.roamly.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.MainActivity
import com.example.roamly.R
import com.example.roamly.data.repository.RoomHolidayRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeFragment :
    Fragment(R.layout.fragment_home) {

    private lateinit var holidayRepository:
            RoomHolidayRepository

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
            RoomHolidayRepository(
                requireContext().applicationContext
            )

        holidayAdapter =
            HolidayAdapter { holiday ->
                (requireActivity() as MainActivity)
                    .showHolidayDetails(
                        holiday.holidayId
                    )
            }

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

            adapter = holidayAdapter
        }

        planHolidayButton.setOnClickListener {
            (requireActivity() as MainActivity)
                .showAddHoliday()
        }

        observeHolidays()
    }

    private fun observeHolidays() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                try {
                    holidayRepository
                        .observeHolidays()
                        .collect { holidays ->
                            val sortedHolidays =
                                holidays.sortedBy {
                                    it.startDate
                                }

                            holidayAdapter.submitList(
                                sortedHolidays
                            )

                            updateEmptyState(
                                sortedHolidays.isEmpty()
                            )
                        }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(
                        "RoamlyHoliday",
                        "Unable to load offline holidays",
                        error
                    )

                    Toast.makeText(
                        requireContext(),
                        "Unable to load saved holidays",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun updateEmptyState(
        isEmpty: Boolean
    ) {
        holidaysRecyclerView.visibility =
            if (isEmpty) {
                View.GONE
            } else {
                View.VISIBLE
            }

        emptyHolidayState.visibility =
            if (isEmpty) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
}
