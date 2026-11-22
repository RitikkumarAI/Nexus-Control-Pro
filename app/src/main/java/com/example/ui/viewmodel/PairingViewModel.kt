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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairingViewModel(application: Application) : AndroidViewModel(application) {

    private val pairingRepository = PairingRepository(application)
    private val firestoreRepository = com.example.data.FirestoreRepository(application)
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
                var targetId: String = ""
                var targetName: String = "Managed Device"
                var targetModel: String = "Android Device"
                var targetManufacturer: String = "Android"
                var targetBattery: Int = 85
                var targetNetwork: String = "4G / 5G"
                var pairingCode: String = ""

                if (qrData.startsWith("{")) {
                    val json = org.json.JSONObject(qrData)
                    targetIp = if (json.has("ip")) json.getString("ip") else null
                    targetPort = json.optInt("port", NexusP2PManager.DEFAULT_PORT)
                    targetId = json.optString("deviceId", "")
                    targetName = json.optString("deviceName", "Managed Device")
                    targetModel = json.optString("model", "Android Device")
                    targetManufacturer = json.optString("manufacturer", "Android")
                    targetBattery = json.optInt("batteryLevel", 85)
                    targetNetwork = json.optString("networkType", "Online")
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
                    if (parts.size >= 4) {
                        targetId = parts[3]
                        pairingCode = parts[2]
                    } else if (parts.size >= 2) {
                        pairingCode = parts[1]
                    }
                }

                if (targetId.isBlank()) {
                    targetId = if (pairingCode.isNotBlank()) "device_$pairingCode" else "device_managed"
                }

                // 1. Construct persistent NexusDevice identity
                val device = NexusDevice(
                    deviceId = targetId,
                    deviceName = targetName,
                    manufacturer = targetManufacturer,
                    model = targetModel,
                    batteryLevel = targetBattery,
                    networkType = targetNetwork,
                    isOnline = true,
                    pairedAdminId = adminDeviceId,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    ipAddress = targetIp,
                    p2pPort = targetPort
                )

                // 2. Persist device pairing locally
                prefsManager.savePairedDeviceId(targetId)

                // 3. Establish cloud pairing in Firestore (Works Agra to Delhi across any Internet connection)
                firestoreRepository.pairDeviceInCloud(
                    managedDeviceId = targetId,
                    adminDeviceId = adminDeviceId,
                    adminDeviceName = adminDeviceName
                )
                firestoreRepository.updateDeviceTelemetry(device)

                if (pairingCode.length == 6) {
                    launch(Dispatchers.IO) {
                        try {
                            firestoreRepository.submitPairingCode(adminDeviceId, adminDeviceName, pairingCode)
                        } catch (e: Exception) {
                            // Non-blocking fallback
                        }
                    }
                }

                _pairingState.value = DevicePairing(
                    pairingId = "pair_$targetId",
                    managedDeviceId = targetId,
                    managedDeviceName = targetName,
                    adminDeviceId = adminDeviceId,
                    adminDeviceName = adminDeviceName,
                    status = PairingStatus.PAIRED.name,
                    managedIp = targetIp,
                    managedPort = targetPort
                )

                // 4. Opportunistic local P2P in background (Does NOT fail cloud pairing if different network)
                if (!targetIp.isNullOrBlank()) {
                    launch(Dispatchers.IO) {
                        try {
                            val p2p = NexusP2PManager.getInstance(getApplication())
                            p2p.pairWithManagedHost(targetIp, targetPort, adminDeviceId, adminDeviceName)
                        } catch (e: Exception) {
                            android.util.Log.d("PairingViewModel", "Local LAN P2P inactive, using Cloud WAN: ${e.message}")
                        }
                    }
                }

                _pairingMessage.value = "Connected to ${device.deviceName} via Cloud Internet!"
                LiveAlertManager.postAlert(
                    title = "Pairing Successful!",
                    message = "Connected to ${device.deviceName} (Cloud Internet Active)",
                    type = LiveAlertType.SUCCESS
                )
                _isLoading.value = false
                onSuccess(device)
            } catch (e: Exception) {
                _pairingMessage.value = "Error parsing QR code: ${e.message}"
                LiveAlertManager.postAlert(
                    title = "Pairing Error",
                    message = "Failed to process QR code: ${e.message}",
                    type = LiveAlertType.ERROR
                )
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
            val current = _pairingState.value
            if (current != null) {
                firestoreRepository.unpairDevice(current.managedDeviceId)
            }
            prefsManager.savePairedDeviceId(null)
            _pairingState.value = null
            _pairingMessage.value = "Device unpaired."
        }
    }
}
