/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.tennistracker.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.tennistracker.R
import com.example.tennistracker.presentation.theme.TennisTrackerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Measurement(
    val x: Float,
    val y: Float,
    val z: Float,
)

class MeasurementViewModel : ViewModel() {
    fun setAccelMeasurement(newMeasurement: Measurement) {
        _accelMeasurement.update { newMeasurement }
    }

    fun setGyroMeasurement(newMeasurement: Measurement) {
        _gyroMeasurement.update { newMeasurement }
    }

    private val _accelMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f))
    val accelMeasurement: StateFlow<Measurement> = _accelMeasurement.asStateFlow()

    private val _gyroMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f))
    val gyroMeasurement: StateFlow<Measurement> = _gyroMeasurement.asStateFlow()
}

class MainActivity :
    ComponentActivity(),
    SensorEventListener {
    companion object {
        const val TAG = "MainActivity"
    }

    lateinit var sensorManager: SensorManager
    lateinit var accelerometer: Sensor
    lateinit var gyroscope: Sensor

    // TODO(robinlinden): Save values, dump to a file on activity exit or something.
    // val accelerometerValues = mutableListOf<Measurement>()
    // val gyroscopeValues = mutableListOf<Measurement>()

    var measurementViewModel: MeasurementViewModel = MeasurementViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(measurementViewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        // TODO(robinlinden): Only add measurement if not very near the last-added one?
        val measurement = Measurement(e.values[0], e.values[1], e.values[2])
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // accelerometerValues.add(measurement)
            measurementViewModel.setAccelMeasurement(measurement)
            Log.w(TAG, "Accelerometer $measurement")
        } else {
            // gyroscopeValues.add(measurement)
            measurementViewModel.setGyroMeasurement(measurement)
            Log.w(TAG, "Gyroscope $measurement")
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int,
    ) {
        Log.e(TAG, "Accuracy changed $sensor $accuracy")
    }
}

@Composable
fun WearApp(measurementViewModel: MeasurementViewModel = MeasurementViewModel()) {
    TennisTrackerTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            TimeText()
            SensorValuesScreen(measurementViewModel)
        }
    }
}

@Composable
fun SensorValuesScreen(measurementViewModel: MeasurementViewModel = MeasurementViewModel()) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DisplayValues(measurementViewModel)
    }
}

@Composable
fun DisplayValues(measurementViewModel: MeasurementViewModel) {
    val measurementData by measurementViewModel.gyroMeasurement.collectAsState()

    Row {
        Column {
            Text(
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Left,
                text = "",
            )
            Text(
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Left,
                text = "X:",
            )
            Text(
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Left,
                text = "Y:",
            )
            Text(
                modifier = Modifier.width(20.dp),
                textAlign = TextAlign.Left,
                text = "Z:",
            )
        }
        DisplayGyroColumn(measurementViewModel)
        DisplayAccelColumn(measurementViewModel)
    }
}

@Composable
fun DisplayGyroColumn(measurementViewModel: MeasurementViewModel) {
    val measurementData by measurementViewModel.gyroMeasurement.collectAsState()

    Column {
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center,
            text = "Gyro",
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurementData.x),
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurementData.y),
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurementData.z),
        )
    }
}

@Composable
fun DisplayAccelColumn(measurementViewModel: MeasurementViewModel) {
    val measurementData by measurementViewModel.accelMeasurement.collectAsState()

    Column {
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center,
            text = "Acc",
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurementData.x),
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurementData.y),
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurementData.z),
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
