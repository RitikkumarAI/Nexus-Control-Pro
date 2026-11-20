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

    fun startScreenShareSession(managedDeviceId: String, adminDeviceId: String) {
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
                managedDeviceId = managedDeviceId,
                adminDeviceId = adminDeviceId,
                sdpOffer = "v=0\r\no=- 482910381029 2 IN IP4 127.0.0.1\r\ns=NexusWebRTCStream\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96"
            )
            _currentSession.value = session
            firestoreRepository.logAuditAction("SCREEN_SHARE_STARTED", "MANAGED", adminDeviceId, "Screen sharing stream initialized via MediaProjection WebRTC.")
        }
    }

    fun stopScreenShareSession() {
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
