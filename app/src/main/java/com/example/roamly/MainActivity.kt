package com.example.roamly

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.roamly.ui.documents.DocumentsFragment
import com.example.roamly.ui.holiday.AddHolidayFragment
import com.example.roamly.ui.home.HomeFragment
import com.example.roamly.ui.itinerary.ItineraryFragment
import com.example.roamly.ui.packing.PackingFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            openFragment(
                HomeFragment(),
                "My Holidays"
            )
        }

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    openFragment(
                        HomeFragment(),
                        "My Holidays"
                    )
                    true
                }

                R.id.nav_itinerary -> {
                    openFragment(
                        ItineraryFragment(),
                        "Itinerary"
                    )
                    true
                }

                R.id.nav_add_holiday -> {
                    openFragment(
                        AddHolidayFragment(),
                        "Add Holiday"
                    )
                    true
                }

                R.id.nav_packing -> {
                    openFragment(
                        PackingFragment(),
                        "Packing"
                    )
                    true
                }

                R.id.nav_documents -> {
                    openFragment(
                        DocumentsFragment(),
                        "Documents"
                    )
                    true
                }

                else -> false
            }
        }
    }

    private fun openFragment(
        fragment: Fragment,
        title: String
    ) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()

        toolbar.title = title
    }
}