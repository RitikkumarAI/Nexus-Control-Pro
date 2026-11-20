package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.data.LocalDeviceManager
import com.example.data.PreferencesManager
import com.example.model.NexusDevice
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefsManager = PreferencesManager(application)
    val firestoreRepository = FirestoreRepository(application)
    val localDeviceManager = LocalDeviceManager(application)

    val roleState: StateFlow<UserRole> = prefsManager.roleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.UNSET)

    val themeModeState: StateFlow<String> = prefsManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefsManager.saveThemeMode(mode)
        }
    }

    private val _currentDeviceId = MutableStateFlow(UUID.randomUUID().toString())
    val currentDeviceId: StateFlow<String> = _currentDeviceId.asStateFlow()

    private val _currentDeviceName = MutableStateFlow("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
    val currentDeviceName: StateFlow<String> = _currentDeviceName.asStateFlow()

    val pairedDeviceIdState: StateFlow<String?> = prefsManager.pairedDeviceIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentDeviceTelemetry = MutableStateFlow<NexusDevice?>(null)
    val currentDeviceTelemetry: StateFlow<NexusDevice?> = _currentDeviceTelemetry.asStateFlow()

    init {
        viewModelScope.launch {
            prefsManager.deviceIdFlow.collect { id ->
                _currentDeviceId.value = id
            }
        }
        viewModelScope.launch {
            prefsManager.deviceNameFlow.collect { name ->
                _currentDeviceName.value = name
            }
        }
    }

    fun selectRole(role: UserRole) {
        viewModelScope.launch {
            prefsManager.saveRole(role)
            firestoreRepository.logAuditAction(
                action = "ROLE_SELECTED",
                initiatorRole = role.name,
                targetDeviceId = currentDeviceId.value,
                details = "User set device operating mode to ${role.name}"
            )
        }
    }

    fun updateLocalTelemetry() {
        val deviceId = currentDeviceId.value
        val deviceName = currentDeviceName.value
        val role = roleState.value.name
        val device = localDeviceManager.getLocalDeviceTelemetry(deviceId, deviceName, role)
        _currentDeviceTelemetry.value = device
        viewModelScope.launch {
            firestoreRepository.updateDeviceTelemetry(device)
        }
    }

    fun revokeAllAccess() {
        viewModelScope.launch {
            prefsManager.savePairedDeviceId(null)
            firestoreRepository.logAuditAction(
                action = "EMERGENCY_REVOKE",
                initiatorRole = roleState.value.name,
                targetDeviceId = currentDeviceId.value,
                details = "Emergency Kill Switch activated — all admin pairings revoked immediately."
            )
        }
    }
}
