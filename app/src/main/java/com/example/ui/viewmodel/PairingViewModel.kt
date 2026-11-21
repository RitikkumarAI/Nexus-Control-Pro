package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LiveAlertManager
import com.example.data.LiveAlertType
import com.example.data.NexusP2PManager
import com.example.data.PairingRepository
import com.example.data.PreferencesManager
import com.example.model.DevicePairing
import com.example.model.NexusDevice
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

    fun pairFromScannedQr(
        qrData: String,
        adminDeviceId: String,
        adminDeviceName: String,
        onSuccess: (NexusDevice) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _pairingMessage.value = "Connecting via QR code..."

            try {
                var targetIp: String? = null
                var targetPort: Int = NexusP2PManager.DEFAULT_PORT
                var targetId: String = "managed_device"
                var targetName: String = "Managed Device"
                var pairingCode: String = ""

                if (qrData.startsWith("{")) {
                    val json = org.json.JSONObject(qrData)
                    targetIp = if (json.has("ip")) json.getString("ip") else null
                    targetPort = json.optInt("port", NexusP2PManager.DEFAULT_PORT)
                    targetId = json.optString("deviceId", "managed_device")
                    targetName = json.optString("deviceName", "Managed Device")
                    pairingCode = json.optString("code", "")
                } else if (qrData.startsWith("NEXUS_P2P:")) {
                    val parts = qrData.split(":")
                    if (parts.size >= 4) {
                        targetIp = parts[1]
                        targetPort = parts[2].toIntOrNull() ?: NexusP2PManager.DEFAULT_PORT
                        targetId = parts[3]
                        if (parts.size >= 5) targetName = parts[4]
                        if (parts.size >= 6) pairingCode = parts[5]
                    }
                } else if (qrData.startsWith("NEXUS_PAIR:")) {
                    val parts = qrData.split(":")
                    if (parts.size >= 2) pairingCode = parts[1]
                    if (parts.size >= 3) targetId = parts[2]
                }

                if (!targetIp.isNullOrBlank()) {
                    val p2p = NexusP2PManager.getInstance(getApplication())
                    val p2pResult = p2p.pairWithManagedHost(targetIp, targetPort, adminDeviceId, adminDeviceName)

                    if (p2pResult.isSuccess) {
                        val device = p2pResult.getOrThrow()
                        prefsManager.savePairedDeviceId(device.deviceId)
                        _pairingState.value = DevicePairing(
                            pairingId = "p2p_" + device.deviceId,
                            managedDeviceId = device.deviceId,
                            managedDeviceName = device.deviceName,
                            adminDeviceId = adminDeviceId,
                            adminDeviceName = adminDeviceName,
                            status = PairingStatus.PAIRED.name,
                            managedIp = targetIp,
                            managedPort = targetPort
                        )
                        _pairingMessage.value = "Connected directly with ${device.deviceName} ($targetIp)!"
                        LiveAlertManager.postAlert(
                            title = "Pairing Successful!",
                            message = "Connected to ${device.deviceName} via QR scan",
                            type = LiveAlertType.SUCCESS
                        )
                        _isLoading.value = false
                        onSuccess(device)
                        return@launch
                    }
                }

                // Fallback to PIN pairing if code was provided
                if (pairingCode.length == 6) {
                    val pairing = pairingRepository.submitPairingCodeFromAdmin(adminDeviceId, adminDeviceName, pairingCode)
                    if (pairing != null) {
                        prefsManager.savePairedDeviceId(pairing.managedDeviceId)
                        _pairingState.value = pairing
                        _pairingMessage.value = "Pairing request verified for ${pairing.managedDeviceName}!"
                        LiveAlertManager.postAlert(
                            title = "Pairing Successful!",
                            message = "Paired with ${pairing.managedDeviceName}",
                            type = LiveAlertType.SUCCESS
                        )
                        _isLoading.value = false
                        onSuccess(
                            NexusDevice(
                                deviceId = pairing.managedDeviceId,
                                deviceName = pairing.managedDeviceName,
                                isOnline = true
                            )
                        )
                        return@launch
                    }
                }

                _pairingMessage.value = "Could not reach device from QR data. Ensure both phones are on same Wi-Fi/Hotspot."
                LiveAlertManager.postAlert(
                    title = "Connection Failed",
                    message = "Could not reach target device. Ensure both phones are on the same Wi-Fi or Hotspot.",
                    type = LiveAlertType.ERROR
                )
            } catch (e: Exception) {
                _pairingMessage.value = "Error parsing QR code: ${e.message}"
            } finally {
                _isLoading.value = false
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
