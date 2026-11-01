
package com.example.roamly

import android.app.Application
import com.example.roamly.data.sync.RoamlySyncScheduler

class RoamlySyncApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        RoamlySyncScheduler.schedule(this)
    }
}
