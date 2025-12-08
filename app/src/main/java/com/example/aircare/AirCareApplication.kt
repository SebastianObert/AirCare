package com.example.aircare

import android.app.Application
import com.example.aircare.util.NotificationHelper

class AirCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
