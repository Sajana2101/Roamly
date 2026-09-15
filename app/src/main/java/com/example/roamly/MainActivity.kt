package com.example.roamly

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.roamly.ui.currency.CurrencyFragment
import com.example.roamly.ui.documents.DocumentsFragment
import com.example.roamly.ui.holiday.AddHolidayFragment
import com.example.roamly.ui.home.HomeFragment
import com.example.roamly.ui.itinerary.ItineraryFragment
import com.example.roamly.ui.packing.PackingFragment
import com.example.roamly.ui.reminders.RemindersFragment
import com.example.roamly.ui.settings.SettingsFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        setSupportActionBar(toolbar)

        val drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )

        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        if (savedInstanceState == null) {
            openFragment(
                HomeFragment(),
                "My Holidays"
            )

            bottomNavigation.selectedItemId = R.id.nav_home
        }

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    openFragment(HomeFragment(), "My Holidays")
                    true
                }

                R.id.nav_itinerary -> {
                    openFragment(ItineraryFragment(), "Itinerary")
                    true
                }

                R.id.nav_add_holiday -> {
                    openFragment(AddHolidayFragment(), "Add Holiday")
                    true
                }

                R.id.nav_packing -> {
                    openFragment(PackingFragment(), "Packing")
                    true
                }

                R.id.nav_documents -> {
                    openFragment(DocumentsFragment(), "Documents")
                    true
                }

                else -> false
            }
        }

        navigationView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.menu_currency -> {
                    openFragment(
                        CurrencyFragment(),
                        "Currency Converter"
                    )
                }

                R.id.menu_reminders -> {
                    openFragment(
                        RemindersFragment(),
                        "Notifications & Reminders"
                    )
                }

                R.id.menu_settings -> {
                    openFragment(
                        SettingsFragment(),
                        "Settings"
                    )
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)

            true
        }
    }

    private fun openFragment(
        fragment: Fragment,
        title: String
    ) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        toolbar.title = title
    }
}