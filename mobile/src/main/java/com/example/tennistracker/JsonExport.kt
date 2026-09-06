package com.example.tennistracker

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
