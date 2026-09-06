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
        val measurements = listOf(
            Measurement(1.0f, 2.0f, 3.0f, 1000L),
            Measurement(4.0f, 5.0f, 6.0f, 2000L),
        )
        val sessions = listOf(
            Session(timestamp = 5000L, measurements = measurements),
        )

        val result = sessionsToJson(sessions)
        val root = JSONArray(result)

        assertEquals(1, root.length())
        val sessionObj = root.getJSONObject(0)
        assertEquals(5000L, sessionObj.getLong("timestamp"))

        val measurementsArray = sessionObj.getJSONArray("measurements")
        assertEquals(2, measurementsArray.length())

        val m1 = measurementsArray.getJSONObject(0)
        assertEquals(1.0, m1.getDouble("x"), 0.001)
        assertEquals(2.0, m1.getDouble("y"), 0.001)
        assertEquals(3.0, m1.getDouble("z"), 0.001)
        assertEquals(1000L, m1.getLong("timestamp"))

        val m2 = measurementsArray.getJSONObject(1)
        assertEquals(4.0, m2.getDouble("x"), 0.001)
        assertEquals(5.0, m2.getDouble("y"), 0.001)
        assertEquals(6.0, m2.getDouble("z"), 0.001)
        assertEquals(2000L, m2.getLong("timestamp"))
    }
}
