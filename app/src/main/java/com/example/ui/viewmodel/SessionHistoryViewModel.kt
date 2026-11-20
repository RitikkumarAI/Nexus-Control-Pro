package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionHistoryEntity
import com.example.data.local.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepository = SessionRepository(application)

    private val _sessionLogs = MutableStateFlow<List<SessionHistoryEntity>>(emptyList())
    val sessionLogs: StateFlow<List<SessionHistoryEntity>> = _sessionLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSessionLogs()
    }

    private fun loadSessionLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            sessionRepository.allSessions.collect { logs ->
                if (logs.isEmpty()) {
                    val initialSample = listOf(
                        SessionHistoryEntity(
                            id = "s1",
                            timestamp = System.currentTimeMillis() - 120000,
                            durationSeconds = 340,
                            interactionType = "SCREEN_SHARE",
                            targetDeviceId = "phone_b",
                            adminDeviceId = "phone_a",
                            details = "Live screen remote assistance session"
                        ),
                        SessionHistoryEntity(
                            id = "s2",
                            timestamp = System.currentTimeMillis() - 600000,
                            durationSeconds = 45,
                            interactionType = "FILE_MANAGER",
                            targetDeviceId = "phone_b",
                            adminDeviceId = "phone_a",
                            details = "File downloaded: report_logs.pdf"
                        ),
                        SessionHistoryEntity(
                            id = "s3",
                            timestamp = System.currentTimeMillis() - 1800000,
                            durationSeconds = 120,
                            interactionType = "LOCATION",
                            targetDeviceId = "phone_b",
                            adminDeviceId = "phone_a",
                            details = "GPS location tracking updated"
                        )
                    )
                    _sessionLogs.value = initialSample
                } else {
                    _sessionLogs.value = logs
                }
                _isLoading.value = false
            }
        }
    }

    fun addSessionLog(interactionType: String, durationSeconds: Long, details: String) {
        viewModelScope.launch {
            sessionRepository.insertSession(
                SessionHistoryEntity(
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = durationSeconds,
                    interactionType = interactionType,
                    details = details
                )
            )
        }
    }

    fun clearSessionHistory() {
        viewModelScope.launch {
            sessionRepository.clearHistory()
        }
    }
}
