package com.example.tennistracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JsonExportTest {
    @Test
    fun sessionsToJson_emptyList() {
        val result = sessionsToJson(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun sessionsToJson_withData() {
        val accelMeasurements = listOf(
            Measurement(1.0f, 2.0f, 3.0f, 1000L),
            Measurement(4.0f, 5.0f, 6.0f, 2000L),
        )

        val gyroMeasurements = listOf(
            Measurement(7.0f, 8.0f, 9.0f, 2500L),
        )

        val sessions = listOf(
            Session(
                timestamp = 5000L,
                accelerometerMeasurements = accelMeasurements,
                gyroscopeMeasurements = gyroMeasurements,
            ),
        )

        val result = sessionsToJson(sessions)
        val root = JSONArray(result)

        assertEquals(1, root.length())
        val sessionObj = root.getJSONObject(0)
        assertEquals(5000L, sessionObj.getLong("timestamp"))

        val accelArray = sessionObj.getJSONArray("accelerometer")
        assertEquals(2, accelArray.length())

        val gyroArray = sessionObj.getJSONArray("gyroscope")
        assertEquals(1, gyroArray.length())

        val m1 = accelArray.getJSONObject(0)
        assertEquals(1.0, m1.getDouble("x"), 0.001)
        assertEquals(2.0, m1.getDouble("y"), 0.001)
        assertEquals(3.0, m1.getDouble("z"), 0.001)
        assertEquals(1000L, m1.getLong("timestamp"))

        val m2 = accelArray.getJSONObject(1)
        assertEquals(4.0, m2.getDouble("x"), 0.001)
        assertEquals(5.0, m2.getDouble("y"), 0.001)
        assertEquals(6.0, m2.getDouble("z"), 0.001)
        assertEquals(2000L, m2.getLong("timestamp"))

        val g1 = gyroArray.getJSONObject(0)
        assertEquals(7.0, g1.getDouble("x"), 0.001)
        assertEquals(8.0, g1.getDouble("y"), 0.001)
        assertEquals(9.0, g1.getDouble("z"), 0.001)
        assertEquals(2500L, g1.getLong("timestamp"))
    }

    @Test
    fun sessionsFromJson_emptyList() {
        assertEquals(0, sessionsFromJson("").size)
        assertEquals(0, sessionsFromJson("[]").size)
    }

    @Test
    fun sessionsToJson_roundTrip() {
        val accelMeasurements = listOf(
            Measurement(1.0f, 2.0f, 3.0f, 1000L),
            Measurement(4.0f, 5.0f, 6.0f, 2000L),
        )

        val gyroMeasurements = listOf(
            Measurement(7.0f, 8.0f, 9.0f, 2500L),
        )

        val sessions = listOf(
            Session(
                timestamp = 5000L,
                accelerometerMeasurements = accelMeasurements,
                gyroscopeMeasurements = gyroMeasurements,
            ),
        )

        val json = sessionsToJson(sessions)
        val deserialized = sessionsFromJson(json)

        assertEquals(1, deserialized.size)
        val session = deserialized[0]
        assertEquals(5000L, session.timestamp)
        assertEquals(2, session.accelerometerMeasurements.size)
        assertEquals(1, session.gyroscopeMeasurements.size)

        assertEquals(1.0f, session.accelerometerMeasurements[0].x)
        assertEquals(2.0f, session.accelerometerMeasurements[0].y)
        assertEquals(3.0f, session.accelerometerMeasurements[0].z)
        assertEquals(1000L, session.accelerometerMeasurements[0].timestamp)

        assertEquals(4.0f, session.accelerometerMeasurements[1].x)
        assertEquals(5.0f, session.accelerometerMeasurements[1].y)
        assertEquals(6.0f, session.accelerometerMeasurements[1].z)
        assertEquals(2000L, session.accelerometerMeasurements[1].timestamp)

        assertEquals(7.0f, session.gyroscopeMeasurements[0].x)
        assertEquals(8.0f, session.gyroscopeMeasurements[0].y)
        assertEquals(9.0f, session.gyroscopeMeasurements[0].z)
        assertEquals(2500L, session.gyroscopeMeasurements[0].timestamp)
    }
}
