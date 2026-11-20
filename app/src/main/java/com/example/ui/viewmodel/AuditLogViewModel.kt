package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.data.local.SessionHistoryRepository
import com.example.model.AuditLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuditLogViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository(application)
    private val sessionHistoryRepository = SessionHistoryRepository(application)

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            firestoreRepository.observeAuditLogs().collect { logs ->
                _auditLogs.value = logs
                _isLoading.value = false
            }
        }
    }

    fun logRemoteAction(action: String, initiatorRole: String, targetDeviceId: String, details: String) {
        viewModelScope.launch {
            // Log to Firestore
            firestoreRepository.logAuditAction(action, initiatorRole, targetDeviceId, details)
            // Log to Room local persistence
            sessionHistoryRepository.logSession(
                interactionType = action,
                durationSeconds = 0L,
                targetDeviceId = targetDeviceId,
                adminDeviceId = initiatorRole,
                details = details
            )
        }
    }
}
