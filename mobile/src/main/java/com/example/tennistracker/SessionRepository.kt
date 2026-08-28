package com.example.tennistracker

import com.example.tennistracker.common.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionRepository {
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    fun addSession(session: Session) {
        _sessions.update { currentSessions ->
            currentSessions + session
        }
    }
}
