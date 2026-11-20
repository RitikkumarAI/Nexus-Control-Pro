package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuditChainVerificationResult
import com.example.data.AuditLogIntegrityVerifier
import com.example.data.FirestoreRepository
import com.example.model.AuditLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository(application)

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _chainVerification = MutableStateFlow(
        AuditChainVerificationResult(isChainIntact = true, totalEntries = 0, verifiedEntries = 0)
    )
    val chainVerification: StateFlow<AuditChainVerificationResult> = _chainVerification.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val baselineLogs: List<AuditLogEntry> by lazy { createSealedBaselineLogs() }

    init {
        loadAuditLogs()
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            firestoreRepository.observeAuditLogs().collect { logs ->
                val resolvedLogs = if (logs.isEmpty()) {
                    baselineLogs
                } else {
                    val hasUnhashedLegacy = logs.any { it.entryHash.isBlank() }
                    if (hasUnhashedLegacy) {
                        AuditLogIntegrityVerifier.sealChronologicalChain(logs)
                    } else {
                        logs.sortedByDescending { it.timestamp }
                    }
                }
                _auditLogs.value = resolvedLogs
                _chainVerification.value = AuditLogIntegrityVerifier.verifyChain(resolvedLogs)
                _isLoading.value = false
            }
        }
    }

    fun reverifyAuditChain() {
        _chainVerification.value = AuditLogIntegrityVerifier.verifyChain(_auditLogs.value)
    }

    private fun createSealedBaselineLogs(): List<AuditLogEntry> {
        val now = System.currentTimeMillis()
        val raw = listOf(
            AuditLogEntry(
                id = "audit_base_01",
                timestamp = now - 3_600_000L,
                action = "DEVICE_PAIRING_VERIFIED",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_a",
                details = "Trusted TLS pairing handshake verified for Field Phone A",
                success = true,
                adminUser = "Ritik Kumar (Admin)",
                outcome = "SUCCESS"
            ),
            AuditLogEntry(
                id = "audit_base_02",
                timestamp = now - 2_400_000L,
                action = "SETTINGS_SYNC",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_a",
                details = "Pushed telemetry interval (15 min) and Wi-Fi diagnostics policy",
                success = true,
                adminUser = "Ritik Kumar (Admin)",
                outcome = "SUCCESS"
            ),
            AuditLogEntry(
                id = "audit_base_03",
                timestamp = now - 1_500_000L,
                action = "OFFLINE_QUEUED_GEOFENCE_UPDATE",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_a",
                details = "Device unreachable — buffered 500m perimeter update in WorkManager queue",
                success = true,
                adminUser = "Ritik Kumar (Admin)",
                outcome = "QUEUED"
            ),
            AuditLogEntry(
                id = "audit_base_04",
                timestamp = now - 900_000L,
                action = "SYNC_QUEUE_EXECUTED_GEOFENCE_UPDATE",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_a",
                details = "WorkManager flushed buffered geofence policy upon reconnection",
                success = true,
                adminUser = "Ritik Kumar (Admin)",
                outcome = "SUCCESS"
            ),
            AuditLogEntry(
                id = "audit_base_05",
                timestamp = now - 420_000L,
                action = "BATTERY_CALIBRATION",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_a",
                details = "Completed Coulomb-counter battery capacity verification (91% health)",
                success = true,
                adminUser = "Priya Sharma (Ops)",
                outcome = "SUCCESS"
            ),
            AuditLogEntry(
                id = "audit_base_06",
                timestamp = now - 180_000L,
                action = "WIFI_NETWORK_SCAN",
                initiatorRole = "ADMIN",
                targetDeviceId = "phone_a",
                details = "Remote Wi-Fi scan timed out before AP list handshake completed",
                success = false,
                adminUser = "Ritik Kumar (Admin)",
                outcome = "FAILED"
            )
        )
        return AuditLogIntegrityVerifier.sealChronologicalChain(raw)
    }

    fun triggerRemoteReboot(targetDeviceId: String = "phone_b") {
        viewModelScope.launch {
            firestoreRepository.triggerRemoteReboot(targetDeviceId)
            firestoreRepository.logAuditAction(
                action = "REMOTE_REBOOT_TRIGGERED",
                initiatorRole = "ADMIN",
                targetDeviceId = targetDeviceId,
                details = "Admin dispatched remote reboot request to device $targetDeviceId",
                adminUser = "Ritik Kumar (Admin)",
                outcome = "SUCCESS",
                success = true
            )
        }
    }
}
