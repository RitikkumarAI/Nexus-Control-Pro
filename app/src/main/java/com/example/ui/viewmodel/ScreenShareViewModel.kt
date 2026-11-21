package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.model.SignalingSession
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScreenShareViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreRepository = FirestoreRepository(application)

    private val _isSharingActive = MutableStateFlow(false)
    val isSharingActive: StateFlow<Boolean> = _isSharingActive.asStateFlow()

    private val _currentSession = MutableStateFlow<SignalingSession?>(null)
    val currentSession: StateFlow<SignalingSession?> = _currentSession.asStateFlow()

    private val _streamResolution = MutableStateFlow("1080p (60 fps)")
    val streamResolution: StateFlow<String> = _streamResolution.asStateFlow()

    private val _connectionLatencyMs = MutableStateFlow(24)
    val connectionLatencyMs: StateFlow<Int> = _connectionLatencyMs.asStateFlow()

    fun requestRemoteScreenStream(targetDeviceId: String, targetIp: String? = null) {
        // Admin requests Managed Device to stream its screen. Admin does NOT capture own screen.
        _isSharingActive.value = true
        viewModelScope.launch {
            if (!targetIp.isNullOrBlank()) {
                val p2p = com.example.data.NexusP2PManager.getInstance(getApplication())
                p2p.sendRemoteCommand(targetIp, "REQUEST_SCREEN_SHARE")
            }
            firestoreRepository.logAuditAction(
                action = "REMOTE_SCREEN_VIEW_REQUESTED",
                initiatorRole = "ADMIN",
                targetDeviceId = targetDeviceId,
                details = "Admin initialized remote screen monitoring of target device."
            )
            com.example.data.LiveAlertManager.postAlert(
                title = "Remote Stream Connected",
                message = "Live screen feed connected to target device ($targetDeviceId)",
                type = com.example.data.LiveAlertType.SUCCESS
            )
        }
    }

    fun stopRemoteViewing(targetDeviceId: String) {
        _isSharingActive.value = false
        viewModelScope.launch {
            firestoreRepository.logAuditAction(
                action = "REMOTE_SCREEN_VIEW_STOPPED",
                initiatorRole = "ADMIN",
                targetDeviceId = targetDeviceId,
                details = "Admin disconnected remote screen viewer session."
            )
        }
    }

    fun startManagedScreenCapture(adminDeviceId: String) {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
        _isSharingActive.value = true

        viewModelScope.launch {
            val session = firestoreRepository.sendSdpOffer(
                pairingId = "pair_active",
                managedDeviceId = "local_managed",
                adminDeviceId = adminDeviceId,
                sdpOffer = "v=0\r\no=- 482910381029 2 IN IP4 127.0.0.1\r\ns=NexusWebRTCStream\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96"
            )
            _currentSession.value = session
            firestoreRepository.logAuditAction("SCREEN_SHARE_STARTED", "MANAGED", adminDeviceId, "Screen sharing stream initialized via MediaProjection WebRTC.")
        }
    }

    fun stopManagedScreenCapture() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        app.startService(intent)
        _isSharingActive.value = false

        viewModelScope.launch {
            val session = _currentSession.value
            if (session != null) {
                firestoreRepository.endSignalingSession(session.sessionId)
                firestoreRepository.logAuditAction("SCREEN_SHARE_STOPPED", "MANAGED", session.adminDeviceId, "Screen sharing stream explicitly terminated.")
            }
        }
    }

    fun startScreenShareSession(managedDeviceId: String, adminDeviceId: String) {
        requestRemoteScreenStream(managedDeviceId)
    }

    fun stopScreenShareSession() {
        _isSharingActive.value = false
    }

    fun setResolution(res: String) {
        _streamResolution.value = res
    }

    private val _focusModeActive = MutableStateFlow(false)
    val focusModeActive: StateFlow<Boolean> = _focusModeActive.asStateFlow()

    private val _ringerVolume = MutableStateFlow(80)
    val ringerVolume: StateFlow<Int> = _ringerVolume.asStateFlow()

    private val _mediaVolume = MutableStateFlow(70)
    val mediaVolume: StateFlow<Int> = _mediaVolume.asStateFlow()

    private val _alarmVolume = MutableStateFlow(85)
    val alarmVolume: StateFlow<Int> = _alarmVolume.asStateFlow()

    fun observeDeviceAudioAndFocus(targetDeviceId: String) {
        viewModelScope.launch {
            firestoreRepository.observeDevice(targetDeviceId).collect { device ->
                if (device != null) {
                    _focusModeActive.value = device.focusModeActive
                    _ringerVolume.value = device.ringerVolume
                    _mediaVolume.value = device.mediaVolume
                    _alarmVolume.value = device.alarmVolume
                }
            }
        }
    }

    fun toggleFocusMode(managedDeviceId: String, enable: Boolean) {
        _focusModeActive.value = enable
        viewModelScope.launch {
            firestoreRepository.sendFocusModeCommand(managedDeviceId, enable)
        }
    }

    fun setRemoteVolume(managedDeviceId: String, ringer: Int?, media: Int?, alarm: Int?) {
        if (ringer != null) _ringerVolume.value = ringer
        if (media != null) _mediaVolume.value = media
        if (alarm != null) _alarmVolume.value = alarm
        viewModelScope.launch {
            firestoreRepository.sendVolumeCommand(managedDeviceId, ringer, media, alarm)
        }
    }
}
