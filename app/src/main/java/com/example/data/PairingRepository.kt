package com.example.data

import android.content.Context
import com.example.model.DevicePairing
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom

class PairingRepository(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context)
) {
    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure 6-digit pairing code on the Managed Device
     */
    suspend fun generateSecurePairingCode(managedDeviceId: String, managedDeviceName: String): DevicePairing {
        val codeInt = 100000 + secureRandom.nextInt(900000)
        val codeStr = codeInt.toString()
        val p2p = NexusP2PManager.getInstance(context)
        val localIp = p2p.getLocalIpAddress()
        p2p.startManagedServer(NexusP2PManager.DEFAULT_PORT)

        return firestoreRepository.generatePairingCode(
            managedDeviceId = managedDeviceId,
            managedDeviceName = managedDeviceName,
            codeOverride = codeStr,
            ipAddress = localIp
        )
    }

    suspend fun submitPairingCodeFromAdmin(adminDeviceId: String, adminDeviceName: String, code: String): DevicePairing? {
        return firestoreRepository.submitPairingCode(adminDeviceId, adminDeviceName, code)
    }

    suspend fun confirmPairingHandshake(pairingId: String, accept: Boolean) {
        firestoreRepository.confirmPairing(pairingId, accept)
    }

    fun observePairing(pairingId: String): Flow<DevicePairing?> {
        return firestoreRepository.observePairing(pairingId)
    }
}
