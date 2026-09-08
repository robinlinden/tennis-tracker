package com.example.tennistracker

import android.app.Application
import com.example.tennistracker.presentation.SensorRepository

class TennisTrackerApplication : Application() {
    val sensorRepository by lazy { SensorRepository() }
}
