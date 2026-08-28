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
                val measurementsArray = json.getJSONArray("measurements")
                val measurements = mutableListOf<Measurement>()
                for (i in 0 until measurementsArray.length()) {
                    val measurement = measurementsArray.getJSONObject(i)
                    measurements.add(
                        Measurement(
                            x = measurement.getDouble("x").toFloat(),
                            y = measurement.getDouble("y").toFloat(),
                            z = measurement.getDouble("z").toFloat(),
                            timestamp = measurement.getLong("timestamp"),
                        ),
                    )
                }

                val session = Session(timestamp = timestamp, measurements = measurements)
                val repository = (application as TennisTrackerApplication).sessionRepository
                repository.addSession(session)
                Log.d("SessionListener", "Received session: $timestamp")
            } catch (e: Exception) {
                Log.e("SessionListener", "Error parsing session data", e)
            }
        }
    }
}
