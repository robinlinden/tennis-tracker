package com.example.tennistracker.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.tennistracker.TennisTrackerApplication
import com.example.tennistracker.common.Measurement
import com.example.tennistracker.common.Session
import com.example.tennistracker.presentation.theme.TennisTrackerTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class MeasurementViewModel(
    repository: SensorRepository,
) : ViewModel() {
    val accelMeasurement: StateFlow<Measurement> = repository.accelMeasurement
    val gyroMeasurement: StateFlow<Measurement> = repository.gyroMeasurement
    val isMeasuring: StateFlow<Boolean> = repository.isMeasuring
    val sessions: StateFlow<List<Session>> = repository.sessions

    private var dataClient: DataClient? = null

    fun setDataClient(client: DataClient) {
        dataClient = client
    }

    fun toggleMeasuring(context: android.content.Context) {
        val wasMeasuring = isMeasuring.value
        val intent = Intent(context, SensorService::class.java)
        if (!wasMeasuring) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
            sessions.value.lastOrNull()?.let { syncSession(it) }
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

class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val rejected = permissions.filterValues { !it }
        if (rejected.isNotEmpty()) {
            Log.e(TAG, "Permissions rejected: ${rejected.keys}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        checkPermissions()

        val repository = (application as TennisTrackerApplication).sensorRepository
        val measurementViewModel = MeasurementViewModel(repository)
        measurementViewModel.setDataClient(Wearable.getDataClient(this))

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(measurementViewModel)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }
}

@Composable
private fun WearApp(measurementViewModel: MeasurementViewModel) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val context = androidx.compose.ui.platform.LocalContext.current

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
                            onToggleMeasuring = { measurementViewModel.toggleMeasuring(context) },
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
    val repository = SensorRepository()
    WearApp(MeasurementViewModel(repository))
}
