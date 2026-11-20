package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PairingRepository
import com.example.data.PreferencesManager
import com.example.model.DevicePairing
import com.example.model.PairingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairingViewModel(application: Application) : AndroidViewModel(application) {

    private val pairingRepository = PairingRepository(application)
    private val prefsManager = PreferencesManager(application)

    private val _pairingState = MutableStateFlow<DevicePairing?>(null)
    val pairingState: StateFlow<DevicePairing?> = _pairingState.asStateFlow()

    private val _inputCode = MutableStateFlow("")
    val inputCode: StateFlow<String> = _inputCode.asStateFlow()

    private val _pairingMessage = MutableStateFlow<String?>(null)
    val pairingMessage: StateFlow<String?> = _pairingMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateInputCode(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _inputCode.value = code
        }
    }

    fun generateManagedPairingCode(managedDeviceId: String, managedDeviceName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val pairing = pairingRepository.generateSecurePairingCode(managedDeviceId, managedDeviceName)
            _pairingState.value = pairing
            _isLoading.value = false

            // Observe status changes
            pairingRepository.observePairing(pairing.pairingId).collect { updated ->
                if (updated != null) {
                    _pairingState.value = updated
                    if (updated.status == PairingStatus.PAIRED.name) {
                        prefsManager.savePairedDeviceId(updated.adminDeviceId)
                    }
                }
            }
        }
    }

    fun submitPairingCodeFromAdmin(adminDeviceId: String, adminDeviceName: String) {
        val code = _inputCode.value
        if (code.length != 6) {
            _pairingMessage.value = "Please enter a valid 6-digit code"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _pairingMessage.value = "Connecting to device..."
            val pairing = pairingRepository.submitPairingCodeFromAdmin(adminDeviceId, adminDeviceName, code)
            _isLoading.value = false

            if (pairing != null) {
                _pairingState.value = pairing
                _pairingMessage.value = "Pairing request sent! Awaiting confirmation on target device."
                prefsManager.savePairedDeviceId(pairing.managedDeviceId)
            } else {
                _pairingMessage.value = "Invalid or expired pairing code. Please try again."
            }
        }
    }

    fun confirmPairingOnManaged(accept: Boolean) {
        val current = _pairingState.value ?: return
        viewModelScope.launch {
            pairingRepository.confirmPairingHandshake(current.pairingId, accept)
            if (accept) {
                prefsManager.savePairedDeviceId(current.adminDeviceId)
                _pairingMessage.value = "Device paired successfully with ${current.adminDeviceName}"
            } else {
                _pairingState.value = null
                _pairingMessage.value = "Pairing request rejected."
            }
        }
    }

    fun unpairDevice() {
        viewModelScope.launch {
            prefsManager.savePairedDeviceId(null)
            _pairingState.value = null
            _pairingMessage.value = "Device unpaired."
        }
    }
}
