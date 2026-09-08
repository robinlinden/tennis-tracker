package com.example.tennistracker.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.tennistracker.TennisTrackerApplication
import com.example.tennistracker.common.Measurement

class SensorService :
    Service(),
    SensorEventListener {
    companion object {
        private const val NOTIFICATION_ID = 1984
        private const val CHANNEL_ID = "sensor_channel"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var repository: SensorRepository

    override fun onCreate() {
        super.onCreate()
        repository = (application as TennisTrackerApplication).sensorRepository
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TennisTracker:SensorWakeLock")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        createNotificationChannel()
        val notification = NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Tennis Tracker")
            .setContentText("Collecting sensor data...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // 3 hours should be enough for anyone, right?
        wakeLock.acquire(3 * 60 * 60 * 1000L)
        registerSensors()
        repository.setIsMeasuring(true)

        return START_STICKY
    }

    private fun registerSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.setIsMeasuring(false)
        unregisterSensors()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {
        val measurement = Measurement(
            event.values[0],
            event.values[1],
            event.values[2],
            System.currentTimeMillis(),
        )

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            repository.setAccelMeasurement(measurement)
            repository.addAccelMeasurement(measurement)
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            repository.setGyroMeasurement(measurement)
            repository.addGyroMeasurement(measurement)
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sensor Collection Channel",
            NotificationManager.IMPORTANCE_LOW,
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
