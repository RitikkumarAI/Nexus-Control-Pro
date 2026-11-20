package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.example.data.FirestoreRepository
import com.example.data.LogcatEntry
import com.example.data.PeerConnectionState
import com.example.data.WebRtcLogcatChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LogcatTargetDevice(
    val deviceId: String,
    val deviceName: String
)

class RemoteLogcatViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val firestoreRepository = FirestoreRepository(application)
    private val logcatChannel = WebRtcLogcatChannel(application, firestoreRepository)

    val availableDevices: List<LogcatTargetDevice> = listOf(
        LogcatTargetDevice("phone_a", "Field Phone A"),
        LogcatTargetDevice("tablet_ops_02", "Warehouse Tablet B2"),
        LogcatTargetDevice("dispatch_unit_03", "Dispatch Pixel C3"),
        LogcatTargetDevice("kiosk_lobby_04", "Lobby Kiosk K4")
    )

    private val _selectedDevice = MutableStateFlow(availableDevices.first())
    val selectedDevice: StateFlow<LogcatTargetDevice> = _selectedDevice.asStateFlow()

    private val _levelFilter = MutableStateFlow("ALL") // ALL, E, W, I, D
    val levelFilter: StateFlow<String> = _levelFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _autoScrollEnabled = MutableStateFlow(true)
    val autoScrollEnabled: StateFlow<Boolean> = _autoScrollEnabled.asStateFlow()

    val connectionState: StateFlow<PeerConnectionState> = logcatChannel.connectionState
    val dataChannelState: StateFlow<String> = logcatChannel.dataChannelState
    val latencyMs: StateFlow<Int> = logcatChannel.latencyMs
    val isPaused: StateFlow<Boolean> = logcatChannel.isPaused
    val logs: StateFlow<List<LogcatEntry>> = logcatChannel.logs

    init {
        connectStream()
    }

    fun selectDevice(device: LogcatTargetDevice) {
        if (_selectedDevice.value.deviceId == device.deviceId) return
        _selectedDevice.value = device
        logcatChannel.clearLogs()
        connectStream()
    }

    fun connectStream() {
        val target = _selectedDevice.value
        logcatChannel.startStream(target.deviceId, target.deviceName)
    }

    fun disconnectStream() {
        logcatChannel.stopStream(logAudit = true)
    }

    fun togglePause() {
        logcatChannel.togglePause()
    }

    fun toggleAutoScroll() {
        _autoScrollEnabled.value = !_autoScrollEnabled.value
    }

    fun setLevelFilter(level: String) {
        _levelFilter.value = level
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearLogs() {
        logcatChannel.clearLogs()
    }

    fun emitDiagnosticProbe() {
        logcatChannel.emitDiagnosticProbe(_selectedDevice.value.deviceId)
    }

    fun shareLogSnapshot(filteredLogs: List<LogcatEntry>) {
        if (filteredLogs.isEmpty()) return
        val report = buildString {
            appendLine("=== NEXUS CONTROL PRO • REMOTE LOGCAT SNAPSHOT ===")
            appendLine("Device: ${_selectedDevice.value.deviceName} (${_selectedDevice.value.deviceId})")
            appendLine("Channel: WebRTC DTLS-SCTP DataChannel (dc:nexus-logcat-v1)")
            appendLine("Entries: ${filteredLogs.size}")
            appendLine()
            filteredLogs.takeLast(120).forEach { entry ->
                appendLine("${entry.timestamp} ${entry.level}/${entry.tag}(${entry.pid}): ${entry.message}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Remote Logcat - ${_selectedDevice.value.deviceName}")
            putExtra(Intent.EXTRA_TEXT, report)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Remote Logcat Snapshot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(chooser)
    }

    override fun onCleared() {
        super.onCleared()
        logcatChannel.stopStream(logAudit = false)
    }
}
