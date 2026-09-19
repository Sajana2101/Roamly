package com.example.roamly

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.roamly.ui.auth.LoginFragment
import com.example.roamly.ui.auth.RegisterFragment
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

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openMainFragment(HomeFragment(), "My Holidays")
                    true
                }

                R.id.nav_itinerary -> {
                    openMainFragment(ItineraryFragment(), "Itinerary")
                    true
                }

                R.id.nav_add_holiday -> {
                    openMainFragment(AddHolidayFragment(), "Add Holiday")
                    true
                }

                R.id.nav_packing -> {
                    openMainFragment(PackingFragment(), "Packing")
                    true
                }

                R.id.nav_documents -> {
                    openMainFragment(DocumentsFragment(), "Documents")
                    true
                }

                else -> false
            }
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_currency -> {
                    openMainFragment(
                        CurrencyFragment(),
                        "Currency Converter"
                    )
                }

                R.id.menu_reminders -> {
                    openMainFragment(
                        RemindersFragment(),
                        "Notifications & Reminders"
                    )
                }

                R.id.menu_settings -> {
                    openMainFragment(
                        SettingsFragment(),
                        "Settings"
                    )
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)

            true
        }

        if (savedInstanceState == null) {
            showLogin()
        } else {
            restoreScreenChrome()
        }
    }

    fun showLogin() {
        showAuthScreen(LoginFragment())
    }

    fun showRegister() {
        showAuthScreen(RegisterFragment())
    }

    fun showHomeAfterAuthentication() {
        showMainNavigation()

        bottomNavigation.selectedItemId = R.id.nav_home

        openMainFragment(
            HomeFragment(),
            "My Holidays"
        )
    }

    private fun showAuthScreen(fragment: Fragment) {
        toolbar.visibility = View.GONE
        bottomNavigation.visibility = View.GONE

        drawerLayout.closeDrawer(GravityCompat.START)
        drawerLayout.setDrawerLockMode(
            DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun showMainNavigation() {
        toolbar.visibility = View.VISIBLE
        bottomNavigation.visibility = View.VISIBLE

        drawerLayout.setDrawerLockMode(
            DrawerLayout.LOCK_MODE_UNLOCKED
        )
    }

    private fun restoreScreenChrome() {
        val currentFragment =
            supportFragmentManager.findFragmentById(
                R.id.fragmentContainer
            )

        if (
            currentFragment is LoginFragment ||
            currentFragment is RegisterFragment
        ) {
            toolbar.visibility = View.GONE
            bottomNavigation.visibility = View.GONE

            drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )
        } else {
            showMainNavigation()
        }
    }

    private fun openMainFragment(
        fragment: Fragment,
        title: String
    ) {
        showMainNavigation()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        toolbar.title = title
    }
}