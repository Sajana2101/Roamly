package com.example.roamly

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.roamly.ui.itinerary.ItineraryFragment
import com.example.roamly.ui.packing.PackingFragment
import com.example.roamly.ui.settings.SettingsFragment
import com.example.roamly.ui.currency.CurrencyFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Temporary navigation buttons used while the main app navigation is still being built
        val packingButton =
            findViewById<Button>(R.id.btnPacking)

        val settingsButton =
            findViewById<Button>(R.id.btnSettings)

        val itineraryButton =
            findViewById<Button>(R.id.btnItinerary)

        val currencyButton =
            findViewById<Button>(
                R.id.btnCurrency
            )

        // Opens the Packing feature
        packingButton.setOnClickListener {

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    PackingFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // Opens the Settings feature
        settingsButton.setOnClickListener {

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    SettingsFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // Opens the Itinerary feature so it can be tested before the final navigation is added
        itineraryButton.setOnClickListener {

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ItineraryFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // Opens the Currency Converter while the final burger-menu navigation is still being built.
        currencyButton.setOnClickListener {

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    CurrencyFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }
}