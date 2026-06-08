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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import com.example.tennistracker.presentation.theme.TennisTrackerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MeasurementViewModel : ViewModel() {
    private val _accelMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val accelMeasurement: StateFlow<Measurement> = _accelMeasurement.asStateFlow()

    private val _gyroMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val gyroMeasurement: StateFlow<Measurement> = _gyroMeasurement.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _capturedAccel = MutableStateFlow<List<Session>>(emptyList())
    val capturedAccel: StateFlow<List<Session>> = _capturedAccel.asStateFlow()

    private val _capturedGyro = MutableStateFlow<List<Session>>(emptyList())
    val capturedGyro: StateFlow<List<Session>> = _capturedGyro.asStateFlow()

    fun setAccelMeasurement(newMeasurement: Measurement) {
        _accelMeasurement.update { newMeasurement }
    }

    fun setGyroMeasurement(newMeasurement: Measurement) {
        _gyroMeasurement.update { newMeasurement }
    }

    fun addAccelMeasurement(measurement: Measurement) {
        _capturedAccel.update { sessions ->
            if (sessions.isEmpty()) return@update sessions
            val currentSession = sessions.last()
            val last = currentSession.measurements.lastOrNull()
            if (last == null ||
                abs(last.x - measurement.x) > 0.1f ||
                abs(last.y - measurement.y) > 0.1f ||
                abs(last.z - measurement.z) > 0.1f ||
                measurement.timestamp - last.timestamp > 10_000
            ) {
                sessions.dropLast(1) + currentSession.copy(measurements = currentSession.measurements + measurement)
            } else {
                sessions
            }
        }
    }

    fun addGyroMeasurement(measurement: Measurement) {
        _capturedGyro.update { sessions ->
            if (sessions.isEmpty()) return@update sessions
            val currentSession = sessions.last()
            val last = currentSession.measurements.lastOrNull()
            if (last == null ||
                abs(last.x - measurement.x) > 0.1f ||
                abs(last.y - measurement.y) > 0.1f ||
                abs(last.z - measurement.z) > 0.1f ||
                measurement.timestamp - last.timestamp > 10_000
            ) {
                sessions.dropLast(1) + currentSession.copy(measurements = currentSession.measurements + measurement)
            } else {
                sessions
            }
        }
    }

    fun toggleMeasuring() {
        _isMeasuring.update { wasMeasuring ->
            val nowMeasuring = !wasMeasuring
            if (nowMeasuring) {
                _capturedAccel.update { it + Session() }
                _capturedGyro.update { it + Session() }
            }
            nowMeasuring
        }
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
        val measurement = Measurement(e.values[0], e.values[1], e.values[2], System.currentTimeMillis())
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (measurementViewModel.isMeasuring.value) {
                measurementViewModel.addAccelMeasurement(measurement)
            }
            measurementViewModel.setAccelMeasurement(measurement)
        } else {
            if (measurementViewModel.isMeasuring.value) {
                measurementViewModel.addGyroMeasurement(measurement)
            }
            measurementViewModel.setGyroMeasurement(measurement)
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
    val pagerState = rememberPagerState(pageCount = { 3 })

    val accelMeasurement by measurementViewModel.accelMeasurement.collectAsState()
    val gyroMeasurement by measurementViewModel.gyroMeasurement.collectAsState()
    val isMeasuring by measurementViewModel.isMeasuring.collectAsState()
    val capturedAccel by measurementViewModel.capturedAccel.collectAsState()
    val capturedGyro by measurementViewModel.capturedGyro.collectAsState()

    TennisTrackerTheme {
        HorizontalPager(state = pagerState) { page ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center,
            ) {
                TimeText()
                when (page) {
                    0 -> {
                        SensorValuesScreen(
                            isMeasuring = isMeasuring,
                            gyroMeasurement = gyroMeasurement,
                            accelMeasurement = accelMeasurement,
                            onToggleMeasuring = { measurementViewModel.toggleMeasuring() },
                        )
                    }

                    1 -> {
                        HistoryScreen("Acc History", capturedAccel)
                    }

                    2 -> {
                        HistoryScreen("Gyro History", capturedGyro)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    title: String,
    sessions: List<Session>,
) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0),
    ) {
        item {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
                text = title,
                style = MaterialTheme.typography.title3,
            )
        }
        sessions.asReversed().forEachIndexed { index, session ->
            item {
                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    text = "Session ${sessions.size - index}",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.secondary,
                )
            }
            items(session.measurements.asReversed()) { m ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = timeFormatter.format(Date(m.timestamp)),
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.secondary,
                    )
                    Text(
                        text = "%.2f, %.2f, %.2f".format(m.x, m.y, m.z),
                        style = MaterialTheme.typography.caption2,
                    )
                }
            }
        }
    }
}

@Composable
fun SensorValuesScreen(
    isMeasuring: Boolean,
    gyroMeasurement: Measurement,
    accelMeasurement: Measurement,
    onToggleMeasuring: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DisplayValues(
            gyroMeasurement = gyroMeasurement,
            accelMeasurement = accelMeasurement,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onToggleMeasuring) {
            Text(if (isMeasuring) "Stop" else "Start")
        }
    }
}

@Composable
fun DisplayValues(
    gyroMeasurement: Measurement,
    accelMeasurement: Measurement,
) {
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
        MeasurementsColumn("Gyro", gyroMeasurement)
        MeasurementsColumn("Acc", accelMeasurement)
    }
}

@Composable
fun MeasurementsColumn(
    title: String,
    measurement: Measurement,
) {
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
