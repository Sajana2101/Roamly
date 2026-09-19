package com.example.roamly

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.ui.auth.LoginFragment
import com.example.roamly.ui.auth.RegisterFragment
import com.example.roamly.ui.currency.CurrencyFragment
import com.example.roamly.ui.documents.DocumentsFragment
import com.example.roamly.ui.holiday.AddHolidayFragment
import com.example.roamly.ui.holiday.EditHolidayFragment
import com.example.roamly.ui.holiday.HolidayDetailsFragment
import com.example.roamly.ui.home.HomeFragment
import com.example.roamly.ui.itinerary.ItineraryFragment
import com.example.roamly.ui.packing.PackingFragment
import com.example.roamly.ui.reminders.RemindersFragment
import com.example.roamly.ui.settings.SettingsFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar:
            MaterialToolbar

    private lateinit var bottomNavigation:
            View

    private lateinit var drawerLayout:
            DrawerLayout

    private lateinit var navigationView:
            NavigationView

    private lateinit var firebaseAuth:
            FirebaseAuth

    private lateinit var sessionManager:
            SessionManager

    private lateinit var navHome:
            View

    private lateinit var navItinerary:
            View

    private lateinit var navAddHoliday:
            View

    private lateinit var navPacking:
            View

    private lateinit var navDocuments:
            View

    private lateinit var navAddHolidayButton:
            View

    private lateinit var navHomeIcon:
            ImageView

    private lateinit var navItineraryIcon:
            ImageView

    private lateinit var navPackingIcon:
            ImageView

    private lateinit var navDocumentsIcon:
            ImageView

    private lateinit var navHomeText:
            TextView

    private lateinit var navItineraryText:
            TextView

    private lateinit var navAddHolidayText:
            TextView

    private lateinit var navPackingText:
            TextView

    private lateinit var navDocumentsText:
            TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        toolbar =
            findViewById(
                R.id.toolbar
            )

        bottomNavigation =
            findViewById(
                R.id.bottomNavigation
            )

        drawerLayout =
            findViewById(
                R.id.drawerLayout
            )

        navigationView =
            findViewById(
                R.id.navigationView
            )

        firebaseAuth =
            FirebaseAuth.getInstance()

        sessionManager =
            SessionManager(
                this
            )

        setupBottomNavigation()

        setSupportActionBar(
            toolbar
        )

        val drawerToggle =
            ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close
            )

        drawerLayout.addDrawerListener(
            drawerToggle
        )

        drawerToggle.syncState()

        toolbar
            .setNavigationOnClickListener {

                drawerLayout.openDrawer(
                    GravityCompat.START
                )
            }

        navigationView
            .setNavigationItemSelectedListener {
                    item ->

                when (
                    item.itemId
                ) {

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

                drawerLayout.closeDrawer(
                    GravityCompat.START
                )

                true
            }

        if (
            savedInstanceState == null
        ) {

            showInitialScreen()

        } else {

            restoreExistingScreen()
        }
    }

    private fun setupBottomNavigation() {

        navHome =
            findViewById(
                R.id.nav_home
            )

        navItinerary =
            findViewById(
                R.id.nav_itinerary
            )

        navAddHoliday =
            findViewById(
                R.id.nav_add_holiday
            )

        navPacking =
            findViewById(
                R.id.nav_packing
            )

        navDocuments =
            findViewById(
                R.id.nav_documents
            )

        navAddHolidayButton =
            findViewById(
                R.id.navAddHolidayButton
            )

        navHomeIcon =
            findViewById(
                R.id.navHomeIcon
            )

        navItineraryIcon =
            findViewById(
                R.id.navItineraryIcon
            )

        navPackingIcon =
            findViewById(
                R.id.navPackingIcon
            )

        navDocumentsIcon =
            findViewById(
                R.id.navDocumentsIcon
            )

        navHomeText =
            findViewById(
                R.id.navHomeText
            )

        navItineraryText =
            findViewById(
                R.id.navItineraryText
            )

        navAddHolidayText =
            findViewById(
                R.id.navAddHolidayText
            )

        navPackingText =
            findViewById(
                R.id.navPackingText
            )

        navDocumentsText =
            findViewById(
                R.id.navDocumentsText
            )

        navHome.setOnClickListener {

            selectBottomNavigation(
                NavigationItem.HOME
            )

            openMainFragment(
                HomeFragment(),
                "My Holidays"
            )
        }

        navItinerary.setOnClickListener {

            selectBottomNavigation(
                NavigationItem.ITINERARY
            )

            openMainFragment(
                ItineraryFragment(),
                "Itinerary"
            )
        }

        navAddHoliday.setOnClickListener {

            openAddHoliday()
        }

        navAddHolidayButton
            .setOnClickListener {

                openAddHoliday()
            }

        navPacking.setOnClickListener {

            selectBottomNavigation(
                NavigationItem.PACKING
            )

            openMainFragment(
                PackingFragment(),
                "Packing"
            )
        }

        navDocuments.setOnClickListener {

            selectBottomNavigation(
                NavigationItem.DOCUMENTS
            )

            openMainFragment(
                DocumentsFragment(),
                "Documents"
            )
        }
    }

    private fun openAddHoliday() {

        selectBottomNavigation(
            NavigationItem.ADD_HOLIDAY
        )

        openMainFragment(
            AddHolidayFragment(),
            "Add Holiday"
        )
    }

    private fun selectBottomNavigation(
        selectedItem: NavigationItem
    ) {

        val activeColor =
            Color.parseColor(
                "#2C79BD"
            )

        val inactiveColor =
            Color.parseColor(
                "#666B72"
            )

        navHomeIcon.imageTintList =
            ColorStateList.valueOf(
                if (
                    selectedItem ==
                    NavigationItem.HOME
                ) {
                    activeColor
                } else {
                    inactiveColor
                }
            )

        navItineraryIcon.imageTintList =
            ColorStateList.valueOf(
                if (
                    selectedItem ==
                    NavigationItem.ITINERARY
                ) {
                    activeColor
                } else {
                    inactiveColor
                }
            )

        navPackingIcon.imageTintList =
            ColorStateList.valueOf(
                if (
                    selectedItem ==
                    NavigationItem.PACKING
                ) {
                    activeColor
                } else {
                    inactiveColor
                }
            )

        navDocumentsIcon.imageTintList =
            ColorStateList.valueOf(
                if (
                    selectedItem ==
                    NavigationItem.DOCUMENTS
                ) {
                    activeColor
                } else {
                    inactiveColor
                }
            )

        navHomeText.setTextColor(
            if (
                selectedItem ==
                NavigationItem.HOME
            ) {
                activeColor
            } else {
                inactiveColor
            }
        )

        navItineraryText.setTextColor(
            if (
                selectedItem ==
                NavigationItem.ITINERARY
            ) {
                activeColor
            } else {
                inactiveColor
            }
        )

        navAddHolidayText.setTextColor(
            if (
                selectedItem ==
                NavigationItem.ADD_HOLIDAY
            ) {
                activeColor
            } else {
                inactiveColor
            }
        )

        navPackingText.setTextColor(
            if (
                selectedItem ==
                NavigationItem.PACKING
            ) {
                activeColor
            } else {
                inactiveColor
            }
        )

        navDocumentsText.setTextColor(
            if (
                selectedItem ==
                NavigationItem.DOCUMENTS
            ) {
                activeColor
            } else {
                inactiveColor
            }
        )

        navHomeText.setTypeface(
            null,
            if (
                selectedItem ==
                NavigationItem.HOME
            ) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.NORMAL
            }
        )

        navItineraryText.setTypeface(
            null,
            if (
                selectedItem ==
                NavigationItem.ITINERARY
            ) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.NORMAL
            }
        )

        navAddHolidayText.setTypeface(
            null,
            if (
                selectedItem ==
                NavigationItem.ADD_HOLIDAY
            ) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.NORMAL
            }
        )

        navPackingText.setTypeface(
            null,
            if (
                selectedItem ==
                NavigationItem.PACKING
            ) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.NORMAL
            }
        )

        navDocumentsText.setTypeface(
            null,
            if (
                selectedItem ==
                NavigationItem.DOCUMENTS
            ) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.NORMAL
            }
        )
    }

    private fun showInitialScreen() {

        if (
            isFullyAuthenticated()
        ) {

            showHomeAfterAuthentication()

        } else {

            clearIncompleteAuthentication()

            showLogin()
        }
    }

    private fun restoreExistingScreen() {

        if (
            isFullyAuthenticated()
        ) {

            val currentFragment =
                supportFragmentManager
                    .findFragmentById(
                        R.id.fragmentContainer
                    )

            if (
                currentFragment is LoginFragment ||
                currentFragment is RegisterFragment
            ) {

                showHomeAfterAuthentication()

            } else {

                restoreScreenChrome()

                restoreBottomNavigation(
                    currentFragment
                )
            }

        } else {

            clearIncompleteAuthentication()

            showLogin()
        }
    }

    private fun restoreBottomNavigation(
        fragment: Fragment?
    ) {

        when (
            fragment
        ) {

            is HomeFragment,
            is HolidayDetailsFragment,
            is EditHolidayFragment -> {

                selectBottomNavigation(
                    NavigationItem.HOME
                )
            }

            is ItineraryFragment -> {

                selectBottomNavigation(
                    NavigationItem.ITINERARY
                )
            }

            is AddHolidayFragment -> {

                selectBottomNavigation(
                    NavigationItem.ADD_HOLIDAY
                )
            }

            is PackingFragment -> {

                selectBottomNavigation(
                    NavigationItem.PACKING
                )
            }

            is DocumentsFragment -> {

                selectBottomNavigation(
                    NavigationItem.DOCUMENTS
                )
            }
        }
    }

    private fun isFullyAuthenticated():
            Boolean {

        val firebaseUserExists =
            firebaseAuth.currentUser != null

        val roamlySessionExists =
            sessionManager.hasSession()

        return firebaseUserExists &&
                roamlySessionExists
    }

    private fun clearIncompleteAuthentication() {

        val firebaseUserExists =
            firebaseAuth.currentUser != null

        val roamlySessionExists =
            sessionManager.hasSession()

        if (
            firebaseUserExists &&
            !roamlySessionExists
        ) {

            firebaseAuth.signOut()
        }

        if (
            !firebaseUserExists &&
            roamlySessionExists
        ) {

            sessionManager.clearSession()
        }
    }

    fun showLogin() {

        showAuthScreen(
            LoginFragment()
        )
    }

    fun showRegister() {

        showAuthScreen(
            RegisterFragment()
        )
    }

    fun showHomeAfterAuthentication() {

        showMainNavigation()

        selectBottomNavigation(
            NavigationItem.HOME
        )

        openMainFragment(
            HomeFragment(),
            "My Holidays"
        )
    }

    fun showAddHoliday() {

        openAddHoliday()
    }

    fun showHome() {

        selectBottomNavigation(
            NavigationItem.HOME
        )

        openMainFragment(
            HomeFragment(),
            "My Holidays"
        )
    }

    fun showHolidayDetails(
        holidayId: Int
    ) {

        selectBottomNavigation(
            NavigationItem.HOME
        )

        openMainFragment(
            HolidayDetailsFragment
                .newInstance(
                    holidayId
                ),
            "Holiday Details"
        )
    }

    fun showEditHoliday(
        holidayId: Int
    ) {

        selectBottomNavigation(
            NavigationItem.HOME
        )

        openMainFragment(
            EditHolidayFragment
                .newInstance(
                    holidayId
                ),
            "Edit Holiday"
        )
    }

    private fun showAuthScreen(
        fragment: Fragment
    ) {

        toolbar.visibility =
            View.GONE

        bottomNavigation.visibility =
            View.GONE

        drawerLayout.closeDrawer(
            GravityCompat.START
        )

        drawerLayout.setDrawerLockMode(
            DrawerLayout
                .LOCK_MODE_LOCKED_CLOSED
        )

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()
    }

    private fun showMainNavigation() {

        toolbar.visibility =
            View.VISIBLE

        bottomNavigation.visibility =
            View.VISIBLE

        drawerLayout.setDrawerLockMode(
            DrawerLayout
                .LOCK_MODE_UNLOCKED
        )
    }

    private fun restoreScreenChrome() {

        val currentFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.fragmentContainer
                )

        if (
            currentFragment is LoginFragment ||
            currentFragment is RegisterFragment
        ) {

            toolbar.visibility =
                View.GONE

            bottomNavigation.visibility =
                View.GONE

            drawerLayout.setDrawerLockMode(
                DrawerLayout
                    .LOCK_MODE_LOCKED_CLOSED
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

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()

        toolbar.title =
            title
    }

    private enum class NavigationItem {
        HOME,
        ITINERARY,
        ADD_HOLIDAY,
        PACKING,
        DOCUMENTS
    }
}