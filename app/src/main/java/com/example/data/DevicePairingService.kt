package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.DevicePairing
import com.example.model.PairingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PairingHandshakeState {
    object Idle : PairingHandshakeState()
    data class CodeGenerated(val pairing: DevicePairing, val qrPayload: String) : PairingHandshakeState()
    data class HandshakeInitiated(val pairing: DevicePairing) : PairingHandshakeState()
    data class TrustedDeviceRegistered(val pairedDeviceId: String, val deviceName: String) : PairingHandshakeState()
    data class Error(val message: String) : PairingHandshakeState()
}

class DevicePairingService(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _handshakeState = MutableStateFlow<PairingHandshakeState>(PairingHandshakeState.Idle)
    val handshakeState: StateFlow<PairingHandshakeState> = _handshakeState.asStateFlow()

    /**
     * 1. Generates unique 6-digit pairing code on Managed Device (Phone A) with QR payload
     */
    fun generatePairingCodeOnManagedDevice(managedDeviceId: String, managedDeviceName: String) {
        scope.launch {
            try {
                val pairing = firestoreRepository.generatePairingCode(managedDeviceId, managedDeviceName)
                val qrPayload = "NEXUS_PAIR:${pairing.pairingId}:${pairing.pairingCode}:${pairing.managedDeviceId}"

                _handshakeState.value = PairingHandshakeState.CodeGenerated(pairing, qrPayload)
                Log.d("DevicePairingService", "Pairing code generated: ${pairing.pairingCode}")

                // Listen for incoming admin connection handshake
                firestoreRepository.observePairing(pairing.pairingId).collect { updated ->
                    if (updated != null) {
                        when (updated.status) {
                            PairingStatus.CONNECTING.name -> {
                                _handshakeState.value = PairingHandshakeState.HandshakeInitiated(updated)
                            }
                            PairingStatus.PAIRED.name -> {
                                preferencesManager.savePairedDeviceId(updated.adminDeviceId)
                                _handshakeState.value = PairingHandshakeState.TrustedDeviceRegistered(
                                    pairedDeviceId = updated.adminDeviceId,
                                    deviceName = updated.adminDeviceName
                                )
                            }
                            PairingStatus.REVOKED.name -> {
                                _handshakeState.value = PairingHandshakeState.Error("Pairing request rejected")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _handshakeState.value = PairingHandshakeState.Error("Failed to generate pairing code: ${e.message}")
            }
        }
    }

    /**
     * 2. Initiates secure Firestore-based handshake from Admin Device (Phone B)
     */
    fun initiateHandshakeFromAdminDevice(adminDeviceId: String, adminDeviceName: String, pairingCode: String) {
        if (pairingCode.length != 6) {
            _handshakeState.value = PairingHandshakeState.Error("Pairing code must be 6 digits")
            return
        }

        scope.launch {
            try {
                val pairing = firestoreRepository.submitPairingCode(adminDeviceId, adminDeviceName, pairingCode)
                if (pairing != null) {
                    _handshakeState.value = PairingHandshakeState.HandshakeInitiated(pairing)

                    // Observe target confirmation
                    firestoreRepository.observePairing(pairing.pairingId).collect { updated ->
                        if (updated?.status == PairingStatus.PAIRED.name) {
                            preferencesManager.savePairedDeviceId(updated.managedDeviceId)
                            _handshakeState.value = PairingHandshakeState.TrustedDeviceRegistered(
                                pairedDeviceId = updated.managedDeviceId,
                                deviceName = updated.managedDeviceName
                            )
                        }
                    }
                } else {
                    _handshakeState.value = PairingHandshakeState.Error("Invalid or expired pairing code")
                }
            } catch (e: Exception) {
                _handshakeState.value = PairingHandshakeState.Error("Handshake failed: ${e.message}")
            }
        }
    }

    /**
     * 3. Confirms trusted registration handshake on Managed Device (Phone A)
     */
    fun confirmHandshakeOnManagedDevice(pairingId: String, accept: Boolean) {
        scope.launch {
            try {
                firestoreRepository.confirmPairing(pairingId, accept)
                if (!accept) {
                    _handshakeState.value = PairingHandshakeState.Idle
                }
            } catch (e: Exception) {
                _handshakeState.value = PairingHandshakeState.Error("Confirmation error: ${e.message}")
            }
        }
    }

    fun observePairing(pairingId: String): Flow<DevicePairing?> {
        return firestoreRepository.observePairing(pairingId)
    }
}
