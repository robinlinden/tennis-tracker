package com.example.tennistracker

import android.util.Log
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class SessionListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (ev in dataEvents) {
            if (ev.type != DataEvent.TYPE_CHANGED || ev.dataItem.uri.path
                    ?.startsWith("/sessions/") == false
            ) {
                continue
            }

            try {
                val jsonData =
                    DataMapItem.fromDataItem(ev.dataItem).dataMap.getString("session_data")
                        ?: throw Exception("Missing session_data")

                val json = JSONObject(jsonData)
                val timestamp = json.getLong("timestamp")

                val accelArray = json.getJSONArray("accelerometer")
                val accelMeasurements = mutableListOf<Measurement>()
                for (i in 0 until accelArray.length()) {
                    val m = accelArray.getJSONObject(i)
                    accelMeasurements.add(
                        Measurement(
                            x = m.getDouble("x").toFloat(),
                            y = m.getDouble("y").toFloat(),
                            z = m.getDouble("z").toFloat(),
                            timestamp = m.getLong("timestamp"),
                        ),
                    )
                }

                val gyroArray = json.getJSONArray("gyroscope")
                val gyroMeasurements = mutableListOf<Measurement>()
                for (i in 0 until gyroArray.length()) {
                    val m = gyroArray.getJSONObject(i)
                    gyroMeasurements.add(
                        Measurement(
                            x = m.getDouble("x").toFloat(),
                            y = m.getDouble("y").toFloat(),
                            z = m.getDouble("z").toFloat(),
                            timestamp = m.getLong("timestamp"),
                        ),
                    )
                }

                val session =
                    Session(
                        timestamp = timestamp,
                        accelerometerMeasurements = accelMeasurements,
                        gyroscopeMeasurements = gyroMeasurements,
                    )
                val repository = (application as TennisTrackerApplication).sessionRepository
                repository.addSession(session)
                Log.d("SessionListener", "Received session: $timestamp")
            } catch (e: Exception) {
                Log.e("SessionListener", "Error parsing session data", e)
            }
        }
    }
}
