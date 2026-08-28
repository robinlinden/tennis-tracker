package com.example.tennistracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.tennistracker.common.Session
import com.example.tennistracker.theme.TennisTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date

class MobileViewModel(
    repository: SessionRepository,
) : ViewModel() {
    val sessions = repository.sessions
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as TennisTrackerApplication).sessionRepository
        val viewModel = MobileViewModel(repository)

        enableEdgeToEdge()
        setContent {
            TennisTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MobileApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun MobileApp(
    viewModel: MobileViewModel,
    modifier: Modifier = Modifier,
) {
    val sessions by viewModel.sessions.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Tennis Tracker - Sessions",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (sessions.isEmpty()) {
            Text(text = "No sessions yet. Start a session on your watch!")
        } else {
            LazyColumn {
                items(sessions.asReversed()) { session ->
                    SessionItem(session)
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: Session) {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", LocalLocale.current.platformLocale)
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${dateFormatter.format(Date(session.timestamp))}",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "${session.measurements.size} measurements",
                style = MaterialTheme.typography.bodyMedium,
            )

            // TODO(robinlinden): Do something less silly here.
            session.measurements.take(3).forEach { m ->
                Text(
                    text = "%.2f, %.2f, %.2f".format(m.x, m.y, m.z),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    TennisTrackerTheme {
        MobileApp(viewModel = MobileViewModel(SessionRepository()))
    }
}
