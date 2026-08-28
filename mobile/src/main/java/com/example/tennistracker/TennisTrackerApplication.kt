package com.example.tennistracker

import android.app.Application

class TennisTrackerApplication : Application() {
    val sessionRepository by lazy { SessionRepository() }
}
