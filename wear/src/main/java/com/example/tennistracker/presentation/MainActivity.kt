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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import com.example.tennistracker.presentation.theme.TennisTrackerTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class MeasurementViewModel : ViewModel() {
    private val _accelMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val accelMeasurement: StateFlow<Measurement> = _accelMeasurement.asStateFlow()

    private val _gyroMeasurement = MutableStateFlow(Measurement(0.0f, 0.0f, 0.0f, 0L))
    val gyroMeasurement: StateFlow<Measurement> = _gyroMeasurement.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private var dataClient: DataClient? = null

    fun setDataClient(client: DataClient) {
        dataClient = client
    }

    fun setAccelMeasurement(newMeasurement: Measurement) {
        _accelMeasurement.update { newMeasurement }
    }

    fun setGyroMeasurement(newMeasurement: Measurement) {
        _gyroMeasurement.update { newMeasurement }
    }

    fun addAccelMeasurement(measurement: Measurement) {
        _sessions.update { sessions ->
            if (sessions.isEmpty()) return@update sessions
            val currentSession = sessions.last()
            val last = currentSession.accelerometerMeasurements.lastOrNull()
            if (last == null ||
                abs(last.x - measurement.x) > 0.1f ||
                abs(last.y - measurement.y) > 0.1f ||
                abs(last.z - measurement.z) > 0.1f ||
                measurement.timestamp - last.timestamp > 10_000
            ) {
                sessions.dropLast(1) + currentSession.copy(
                    accelerometerMeasurements = currentSession.accelerometerMeasurements + measurement,
                )
            } else {
                sessions
            }
        }
    }

    fun addGyroMeasurement(measurement: Measurement) {
        _sessions.update { sessions ->
            if (sessions.isEmpty()) return@update sessions
            val currentSession = sessions.last()
            val last = currentSession.gyroscopeMeasurements.lastOrNull()
            if (last == null ||
                abs(last.x - measurement.x) > 0.1f ||
                abs(last.y - measurement.y) > 0.1f ||
                abs(last.z - measurement.z) > 0.1f ||
                measurement.timestamp - last.timestamp > 10_000
            ) {
                sessions.dropLast(1) + currentSession.copy(
                    gyroscopeMeasurements = currentSession.gyroscopeMeasurements + measurement,
                )
            } else {
                sessions
            }
        }
    }

    fun toggleMeasuring() {
        _isMeasuring.update { wasMeasuring ->
            val nowMeasuring = !wasMeasuring
            if (nowMeasuring) {
                _sessions.update { it + Session() }
            } else {
                _sessions.value.lastOrNull()?.let { syncSession(it) }
            }
            nowMeasuring
        }
    }

    private fun syncSession(session: Session) {
        val client = dataClient ?: return

        val json =
            JSONObject().apply {
                put("timestamp", session.timestamp)

                val accelArray = JSONArray()
                session.accelerometerMeasurements.forEach { m ->
                    accelArray.put(
                        JSONObject().apply {
                            put("x", m.x)
                            put("y", m.y)
                            put("z", m.z)
                            put("timestamp", m.timestamp)
                        },
                    )
                }
                put("accelerometer", accelArray)

                val gyroArray = JSONArray()
                session.gyroscopeMeasurements.forEach { m ->
                    gyroArray.put(
                        JSONObject().apply {
                            put("x", m.x)
                            put("y", m.y)
                            put("z", m.z)
                            put("timestamp", m.timestamp)
                        },
                    )
                }
                put("gyroscope", gyroArray)
            }

        val putRequest =
            PutDataMapRequest.create("/sessions/${session.timestamp}").run {
                dataMap.putString("session_data", json.toString())
                dataMap.putLong("timestamp", System.currentTimeMillis())
                asPutDataRequest().apply {
                    setUrgent()
                }
            }

        client
            .putDataItem(putRequest)
            .addOnSuccessListener { Log.d("Sync", "Successfully synced session ${session.timestamp}") }
            .addOnFailureListener { Log.e("Sync", "Failed to sync session ${session.timestamp}", it) }
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

        measurementViewModel.setDataClient(Wearable.getDataClient(this))

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
private fun WearApp(measurementViewModel: MeasurementViewModel = MeasurementViewModel()) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    val accelMeasurement by measurementViewModel.accelMeasurement.collectAsState()
    val gyroMeasurement by measurementViewModel.gyroMeasurement.collectAsState()
    val isMeasuring by measurementViewModel.isMeasuring.collectAsState()
    val sessions by measurementViewModel.sessions.collectAsState()

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
                        HistoryScreen(sessions)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(sessions: List<Session>) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0),
    ) {
        item {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
                text = "Session History",
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
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Acc: ${session.accelerometerMeasurements.size} samples",
                        style = MaterialTheme.typography.caption2,
                    )
                    Text(
                        text = "Gyro: ${session.gyroscopeMeasurements.size} samples",
                        style = MaterialTheme.typography.caption2,
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorValuesScreen(
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
private fun DisplayValues(
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
private fun MeasurementsColumn(
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
private fun DefaultPreview() {
    WearApp()
}
