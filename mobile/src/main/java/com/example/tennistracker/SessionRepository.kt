package com.example.tennistracker

import android.content.Context
import android.util.Log
import com.example.tennistracker.common.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

// TODO(robinlinden): Propagate errors to the UI.
class SessionRepository(
    context: Context? = null,
) {
    private val file: File? = context?.let { File(it.filesDir, "sessions.json") }

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private fun loadSessions(): List<Session> {
        val f = file ?: return emptyList()

        if (!f.exists()) return emptyList()

        return try {
            val jsonStr = f.readText()
            sessionsFromJson(jsonStr)
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error loading sessions", e)
            emptyList()
        }
    }

    private fun saveSessions(sessions: List<Session>) {
        val f = file ?: return
        try {
            val jsonStr = sessionsToJson(sessions)
            f.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e("SessionRepository", "Error saving sessions", e)
        }
    }

    fun addSession(session: Session) {
        _sessions.update { currentSessions ->
            val updated = currentSessions + session
            saveSessions(updated)
            updated
        }
    }
}
