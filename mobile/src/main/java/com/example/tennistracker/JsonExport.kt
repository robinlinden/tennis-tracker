package com.example.tennistracker

import com.example.tennistracker.common.Session
import org.json.JSONArray
import org.json.JSONObject

fun sessionsToJson(sessions: List<Session>): String {
    val root = JSONArray()

    sessions.forEach { session ->
        val sessionObject = JSONObject()
        sessionObject.put("timestamp", session.timestamp)

        val measurementsArray = JSONArray()
        session.measurements.forEach { m ->
            val measurementObject = JSONObject().apply {
                put("x", m.x)
                put("y", m.y)
                put("z", m.z)
                put("timestamp", m.timestamp)
            }

            measurementsArray.put(measurementObject)
        }

        sessionObject.put("measurements", measurementsArray)
        root.put(sessionObject)
    }

    return root.toString(2)
}
