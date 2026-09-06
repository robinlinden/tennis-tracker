package com.example.tennistracker

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class MobileAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: SessionRepository
    private lateinit var viewModel: MobileViewModel

    @Before
    fun setup() {
        Intents.init()
        repository = SessionRepository()
        viewModel = MobileViewModel(repository)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun exportButton_notShownWhenEmpty() {
        composeTestRule.setContent {
            MobileApp(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Export All").assertDoesNotExist()
    }

    @Test
    fun exportButton_shownWhenHasData() {
        repository.addSession(Session(measurements = listOf(Measurement(1f, 1f, 1f, 0L))))

        composeTestRule.setContent {
            MobileApp(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Export All").assertExists()
    }

    @Test
    fun exportFlow_writesCorrectData() {
        val testSession = Session(timestamp = 12345L, measurements = listOf(Measurement(1.2f, 3.4f, 5.6f, 6789L)))
        repository.addSession(testSession)

        // Prepare a temporary file to act as the "selected" file.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFile = File(context.cacheDir, "test_export.json")
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()
        val tempUri = Uri.fromFile(tempFile)

        // Mock the result of the CREATE_DOCUMENT intent.
        val resultData = Intent().apply {
            data = tempUri
        }
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(result)

        composeTestRule.setContent {
            MobileApp(viewModel = viewModel)
        }

        // Trigger export.
        composeTestRule.onNodeWithText("Export All").performClick()

        // Wait for potential async operations.
        composeTestRule.waitForIdle()

        // Verify file content.
        assertTrue("Exported file should exist", tempFile.exists())
        val content = tempFile.readText()

        val jsonArray = JSONArray(content)
        assertEquals(1, jsonArray.length())

        val sessionJson = jsonArray.getJSONObject(0)
        assertEquals(12345L, sessionJson.getLong("timestamp"))

        val measurementsJson = sessionJson.getJSONArray("measurements")
        assertEquals(1, measurementsJson.length())

        val measurementJson = measurementsJson.getJSONObject(0)
        assertEquals(1.2, measurementJson.getDouble("x"), 0.0001)
        assertEquals(3.4, measurementJson.getDouble("y"), 0.0001)
        assertEquals(5.6, measurementJson.getDouble("z"), 0.0001)
        assertEquals(6789L, measurementJson.getLong("timestamp"))
    }
}
