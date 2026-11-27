package com.example.tennistracker

import android.util.Log
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import org.json.JSONArray
import org.json.JSONObject

fun sessionsToJson(sessions: List<Session>): String {
    val root = JSONArray()

    sessions.forEach { session ->
        val sessionObject = JSONObject()
        sessionObject.put("timestamp", session.timestamp)

        val accelArray = JSONArray()
        session.accelerometerMeasurements.forEach { m ->
            accelArray.put(
                JSONObject().apply {
                    put("x", m.x)
                    put("y", m.y)
                    put("z", m.z)
                    put("timestamp", m.timestamp)
                },
            )
        }
        sessionObject.put("accelerometer", accelArray)

        val gyroArray = JSONArray()
        session.gyroscopeMeasurements.forEach { m ->
            gyroArray.put(
                JSONObject().apply {
                    put("x", m.x)
                    put("y", m.y)
                    put("z", m.z)
                    put("timestamp", m.timestamp)
                },
            )
        }
        sessionObject.put("gyroscope", gyroArray)

        root.put(sessionObject)
    }

    return root.toString(2)
}

fun sessionsFromJson(jsonStr: String): List<Session> =
    try {
        val sessions = mutableListOf<Session>()
        val root = JSONArray(jsonStr)
        for (i in 0 until root.length()) {
            val sessionObj = root.getJSONObject(i)
            val timestamp = sessionObj.optLong("timestamp", System.currentTimeMillis())

            val accelArray = sessionObj.optJSONArray("accelerometer") ?: JSONArray()
            val accelMeasurements = mutableListOf<Measurement>()
            for (j in 0 until accelArray.length()) {
                val m = accelArray.getJSONObject(j)
                accelMeasurements.add(
                    Measurement(
                        x = m.optDouble("x", 0.0).toFloat(),
                        y = m.optDouble("y", 0.0).toFloat(),
                        z = m.optDouble("z", 0.0).toFloat(),
                        timestamp = m.optLong("timestamp", 0L),
                    ),
                )
            }

            val gyroArray = sessionObj.optJSONArray("gyroscope") ?: JSONArray()
            val gyroMeasurements = mutableListOf<Measurement>()
            for (j in 0 until gyroArray.length()) {
                val m = gyroArray.getJSONObject(j)
                gyroMeasurements.add(
                    Measurement(
                        x = m.optDouble("x", 0.0).toFloat(),
                        y = m.optDouble("y", 0.0).toFloat(),
                        z = m.optDouble("z", 0.0).toFloat(),
                        timestamp = m.optLong("timestamp", 0L),
                    ),
                )
            }

            sessions.add(
                Session(
                    timestamp = timestamp,
                    accelerometerMeasurements = accelMeasurements,
                    gyroscopeMeasurements = gyroMeasurements,
                ),
            )
        }

        sessions
    } catch (e: Exception) {
        Log.e("SessionRepository", "Error parsing session data", e)
        emptyList()
    }
