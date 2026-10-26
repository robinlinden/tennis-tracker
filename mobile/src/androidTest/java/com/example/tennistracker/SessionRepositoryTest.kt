package com.example.tennistracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {
    @Test
    fun sessionRepository_persistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clean up any existing file
        File(context.filesDir, "sessions.json").delete()

        val repo1 = SessionRepository(context)
        assertEquals(0, repo1.sessions.value.size)

        val testSession = Session(
            timestamp = 9999L,
            accelerometerMeasurements = listOf(Measurement(1f, 2f, 3f, 123L)),
        )
        repo1.addSession(testSession)

        // Create a new repository instance pointing to the same context/filesDir
        val repo2 = SessionRepository(context)
        assertEquals(1, repo2.sessions.value.size)
        val loadedSession = repo2.sessions.value[0]
        assertEquals(9999L, loadedSession.timestamp)
        assertEquals(1, loadedSession.accelerometerMeasurements.size)
        assertEquals(1f, loadedSession.accelerometerMeasurements[0].x)
        assertEquals(2f, loadedSession.accelerometerMeasurements[0].y)
        assertEquals(3f, loadedSession.accelerometerMeasurements[0].z)
        assertEquals(123L, loadedSession.accelerometerMeasurements[0].timestamp)
    }
}
