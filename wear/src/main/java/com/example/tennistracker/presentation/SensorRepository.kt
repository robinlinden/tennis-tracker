package com.example.tennistracker.presentation

import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class SensorRepository {
    private val _accelMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val accelMeasurement: StateFlow<Measurement> = _accelMeasurement.asStateFlow()

    private val _gyroMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val gyroMeasurement: StateFlow<Measurement> = _gyroMeasurement.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    fun setAccelMeasurement(newMeasurement: Measurement) {
        _accelMeasurement.update { newMeasurement }
    }

    fun setGyroMeasurement(newMeasurement: Measurement) {
        _gyroMeasurement.update { newMeasurement }
    }

    fun setIsMeasuring(measuring: Boolean) {
        _isMeasuring.update { measuring }
        if (measuring) {
            _sessions.update { it + Session() }
        }
    }

    fun addAccelMeasurement(measurement: Measurement) {
        _sessions.update { sessions ->
            if (sessions.isEmpty()) return@update sessions
            val currentSession = sessions.last()
            val last = currentSession.accelerometerMeasurements.lastOrNull()
            if (last == null ||
                abs(last.x - measurement.x) > 0.1f ||
                abs(last.y - measurement.y) > 0.1f ||
                abs(last.z - measurement.z) > 0.1f ||
                measurement.timestamp - last.timestamp > 10_000
            ) {
                sessions.dropLast(1) + currentSession.copy(
                    accelerometerMeasurements = currentSession.accelerometerMeasurements + measurement,
                )
            } else {
                sessions
            }
        }
    }

    fun addGyroMeasurement(measurement: Measurement) {
        _sessions.update { sessions ->
            if (sessions.isEmpty()) return@update sessions
            val currentSession = sessions.last()
            val last = currentSession.gyroscopeMeasurements.lastOrNull()
            if (last == null ||
                abs(last.x - measurement.x) > 0.1f ||
                abs(last.y - measurement.y) > 0.1f ||
                abs(last.z - measurement.z) > 0.1f ||
                measurement.timestamp - last.timestamp > 10_000
            ) {
                sessions.dropLast(1) + currentSession.copy(
                    gyroscopeMeasurements = currentSession.gyroscopeMeasurements + measurement,
                )
            } else {
                sessions
            }
        }
    }
}
