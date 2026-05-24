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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.presentation.theme.TennisTrackerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MeasurementViewModel : ViewModel() {
    private val _accelMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val accelMeasurement: StateFlow<Measurement> = _accelMeasurement.asStateFlow()

    private val _gyroMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val gyroMeasurement: StateFlow<Measurement> = _gyroMeasurement.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    fun setAccelMeasurement(newMeasurement: Measurement) {
        _accelMeasurement.update { newMeasurement }
    }

    fun setGyroMeasurement(newMeasurement: Measurement) {
        _gyroMeasurement.update { newMeasurement }
    }

    fun toggleMeasuring() {
        _isMeasuring.update { !it }
    }
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

    // TODO(robinlinden): Dump to a file on activity exit or something.
    val accelerometerValues = mutableListOf<Measurement>()
    val gyroscopeValues = mutableListOf<Measurement>()

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
        val measurement = Measurement(e.values[0], e.values[1], e.values[2], System.currentTimeMillis())
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (measurementViewModel.isMeasuring.value) {
                accelerometerValues.add(measurement)
            }
            measurementViewModel.setAccelMeasurement(measurement)
            Log.w(TAG, "Accelerometer $measurement")
        } else {
            if (measurementViewModel.isMeasuring.value) {
                gyroscopeValues.add(measurement)
            }
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
    val isMeasuring by measurementViewModel.isMeasuring.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DisplayValues(measurementViewModel)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { measurementViewModel.toggleMeasuring() }) {
            Text(if (isMeasuring) "Stop" else "Start")
        }
    }
}

@Composable
fun DisplayValues(measurementViewModel: MeasurementViewModel) {
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
        MeasurementsColumn("Gyro", measurementViewModel.gyroMeasurement)
        MeasurementsColumn("Acc", measurementViewModel.accelMeasurement)
    }
}

@Composable
fun MeasurementsColumn(
    title: String,
    measurements: StateFlow<Measurement>,
) {
    val measurement by measurements.collectAsState()

    Column {
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Center,
            text = title,
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurement.x),
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurement.y),
        )
        Text(
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.Right,
            text = "%.2f".format(measurement.z),
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
