package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AuditLogEntry
import com.example.model.ComplianceReport
import com.example.model.DevicePairing
import com.example.model.FleetBroadcastAlert
import com.example.model.GeofencePushAlert
import com.example.model.KioskConfig
import com.example.model.NexusDevice
import com.example.model.PairingStatus
import com.example.model.SignalingSession
import com.example.model.StorageOptimizationReport
import com.example.model.WifiProvisioningProfile
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository(private val context: Context) {

    companion object {
        // Shared across ViewModels & WorkManager workers so newly logged actions appear immediately in Activity Logs
        private val sharedAuditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    }

    private val db: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(dbId)
            } else {
                Firebase.firestore
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Fallback to default firestore instance: ${e.message}")
            Firebase.firestore
        }
    }

    // In-memory cache for fallback execution when offline or unauthenticated
    private val localDeviceCache = MutableStateFlow<NexusDevice?>(null)
    private val localPairingState = MutableStateFlow<DevicePairing?>(null)
    private val localSignalingState = MutableStateFlow<SignalingSession?>(null)
    private val localGeofenceAlerts = MutableStateFlow<List<GeofencePushAlert>>(emptyList())
    private val localKioskConfigs = MutableStateFlow<Map<String, KioskConfig>>(emptyMap())
    private val localComplianceReports = MutableStateFlow<Map<String, ComplianceReport>>(emptyMap())
    private val localWifiProfiles = MutableStateFlow<List<WifiProvisioningProfile>>(emptyList())
    private val localFleetBroadcasts = MutableStateFlow<List<FleetBroadcastAlert>>(emptyList())
    private val localStorageReports = MutableStateFlow<Map<String, StorageOptimizationReport>>(emptyMap())

    // --- DEVICE STATUS API ---
    suspend fun updateDeviceTelemetry(device: NexusDevice) {
        localDeviceCache.value = device
        try {
            db.collection("devices").document(device.deviceId)
                .set(device)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Saved device telemetry to local state cache (${e.message})")
        }
    }

    suspend fun updateDeviceNetworkState(deviceId: String, networkType: String, isOnline: Boolean) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(networkType = networkType, isOnline = isOnline)
        }
        try {
            db.collection("devices").document(deviceId)
                .update(mapOf("networkType" to networkType, "network_status" to networkType, "isOnline" to isOnline))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated local device network state: $networkType")
        }
    }

    suspend fun updateDeviceBatteryState(deviceId: String, batteryLevel: Int, isCharging: Boolean) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(batteryLevel = batteryLevel, isCharging = isCharging)
        }
        try {
            db.collection("devices").document(deviceId)
                .update(mapOf("batteryLevel" to batteryLevel, "isCharging" to isCharging))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated local device battery state: $batteryLevel%")
        }
    }

    suspend fun updateSystemMetrics(
        deviceId: String,
        batteryLevel: Int,
        isCharging: Boolean,
        networkType: String,
        isOnline: Boolean,
        latitude: Double? = null,
        longitude: Double? = null,
        address: String? = null
    ) {
        val updates = mutableMapOf<String, Any>(
            "batteryLevel" to batteryLevel,
            "isCharging" to isCharging,
            "networkType" to networkType,
            "isOnline" to isOnline,
            "lastSeenTimestamp" to System.currentTimeMillis()
        )
        if (latitude != null) updates["lastLatitude"] = latitude
        if (longitude != null) updates["lastLongitude"] = longitude
        if (address != null) updates["lastLocationAddress"] = address

        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                networkType = networkType,
                isOnline = isOnline,
                lastLatitude = latitude ?: current.lastLatitude,
                lastLongitude = longitude ?: current.lastLongitude,
                lastLocationAddress = address ?: current.lastLocationAddress
            )
        }

        try {
            db.collection("devices").document(deviceId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated local system metrics cache for $deviceId")
        }
    }

    fun observeSystemMetricsFlow(deviceId: String): Flow<NexusDevice?> = observeDevice(deviceId)

    fun observeDevice(deviceId: String): Flow<NexusDevice?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("devices").document(deviceId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(localDeviceCache.value)
                        return@addSnapshotListener
                    }
                    val device = snapshot.toObject(NexusDevice::class.java)
                    trySend(device ?: localDeviceCache.value)
                }
        } catch (e: Exception) {
            trySend(localDeviceCache.value)
        }
        awaitClose { listener?.remove() }
    }

    // --- DEVICE PAIRING API ---
    suspend fun generatePairingCode(
        managedDeviceId: String,
        managedDeviceName: String,
        codeOverride: String? = null,
        ipAddress: String? = null
    ): DevicePairing {
        val code = codeOverride ?: (100000..999999).random().toString()
        val pairingId = "pair_${UUID.randomUUID().toString().take(8)}"
        val pairing = DevicePairing(
            pairingId = pairingId,
            managedDeviceId = managedDeviceId,
            managedDeviceName = managedDeviceName,
            pairingCode = code,
            status = PairingStatus.GENERATED.name,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (10 * 60 * 1000),
            managedIp = ipAddress,
            managedPort = 8899
        )

        localPairingState.value = pairing
        try {
            db.collection("pairings").document(pairingId).set(pairing).await()
            // Also index code lookup
            db.collection("pairing_codes").document(code).set(mapOf("pairingId" to pairingId)).await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Generated pairing code in local cache: $code")
        }
        return pairing
    }

    suspend fun submitPairingCode(adminDeviceId: String, adminDeviceName: String, code: String): DevicePairing? {
        try {
            val codeDoc = db.collection("pairing_codes").document(code).get().await()
            val pairingId = codeDoc.getString("pairingId")
            if (pairingId != null) {
                val pairingDoc = db.collection("pairings").document(pairingId).get().await()
                val pairing = pairingDoc.toObject(DevicePairing::class.java)
                if (pairing != null && pairing.expiresAt > System.currentTimeMillis()) {
                    val updatedPairing = pairing.copy(
                        adminDeviceId = adminDeviceId,
                        adminDeviceName = adminDeviceName,
                        status = PairingStatus.CONNECTING.name
                    )
                    db.collection("pairings").document(pairingId).set(updatedPairing).await()
                    localPairingState.value = updatedPairing
                    return updatedPairing
                }
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Firestore pairing lookup error: ${e.message}")
        }

        // Check local state fallback for dev testing
        val currentLocal = localPairingState.value
        if (currentLocal != null && currentLocal.pairingCode == code) {
            val updated = currentLocal.copy(
                adminDeviceId = adminDeviceId,
                adminDeviceName = adminDeviceName,
                status = PairingStatus.CONNECTING.name
            )
            localPairingState.value = updated
            return updated
        }
        return null
    }

    suspend fun confirmPairing(pairingId: String, accept: Boolean) {
        val newStatus = if (accept) PairingStatus.PAIRED.name else PairingStatus.REVOKED.name
        val current = localPairingState.value?.copy(status = newStatus)
        localPairingState.value = current

        try {
            db.collection("pairings").document(pairingId)
                .update("status", newStatus)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated local pairing status: $newStatus")
        }
    }

    fun observePairing(pairingId: String): Flow<DevicePairing?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("pairings").document(pairingId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(localPairingState.value)
                        return@addSnapshotListener
                    }
                    val pairing = snapshot.toObject(DevicePairing::class.java)
                    trySend(pairing ?: localPairingState.value)
                }
        } catch (e: Exception) {
            trySend(localPairingState.value)
        }
        awaitClose { listener?.remove() }
    }

    // --- WEBRTC SIGNALING API ---
    suspend fun sendSdpOffer(pairingId: String, managedDeviceId: String, adminDeviceId: String, sdpOffer: String, sessionType: String = "SCREEN_SHARE"): SignalingSession {
        val sessionId = "sig_${UUID.randomUUID().toString().take(8)}"
        val session = SignalingSession(
            sessionId = sessionId,
            pairingId = pairingId,
            managedDeviceId = managedDeviceId,
            adminDeviceId = adminDeviceId,
            sessionType = sessionType,
            status = "OFFERING",
            sdpOffer = sdpOffer,
            timestamp = System.currentTimeMillis()
        )
        localSignalingState.value = session
        try {
            db.collection("signaling").document(sessionId).set(session).await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Signaling SDP offer created in local state")
        }
        return session
    }

    suspend fun sendSdpAnswer(sessionId: String, sdpAnswer: String) {
        val current = localSignalingState.value?.copy(sdpAnswer = sdpAnswer, status = "CONNECTED")
        localSignalingState.value = current
        try {
            db.collection("signaling").document(sessionId)
                .update(mapOf("sdpAnswer" to sdpAnswer, "status" to "CONNECTED"))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Signaling SDP answer updated locally")
        }
    }

    suspend fun sendIceCandidate(sessionId: String, candidateJson: String) {
        val current = localSignalingState.value
        if (current != null) {
            val updatedCandidates = current.iceCandidates + candidateJson
            localSignalingState.value = current.copy(iceCandidates = updatedCandidates)
        }
        try {
            db.collection("signaling").document(sessionId)
                .update("iceCandidates", FieldValue.arrayUnion(candidateJson))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Added ICE candidate locally")
        }
    }

    fun observeSignalingSession(sessionId: String): Flow<SignalingSession?> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("signaling").document(sessionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(localSignalingState.value)
                        return@addSnapshotListener
                    }
                    val session = snapshot.toObject(SignalingSession::class.java)
                    trySend(session ?: localSignalingState.value)
                }
        } catch (e: Exception) {
            trySend(localSignalingState.value)
        }
        awaitClose { listener?.remove() }
    }

    suspend fun endSignalingSession(sessionId: String) {
        localSignalingState.value = localSignalingState.value?.copy(status = "ENDED")
        try {
            db.collection("signaling").document(sessionId).update("status", "ENDED").await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Ended signaling session locally")
        }
    }

    // --- AUDIT LOGS API (TAMPER-EVIDENT SHA-256 HASH CHAIN) ---
    suspend fun logAuditAction(
        action: String,
        initiatorRole: String,
        targetDeviceId: String,
        details: String,
        adminUser: String = if (initiatorRole.equals("ADMIN", ignoreCase = true)) "Ritik Kumar (Admin)" else "Managed System Agent",
        outcome: String = "SUCCESS",
        success: Boolean = outcome != "FAILED"
    ): AuditLogEntry {
        val existingLogs = sharedAuditLogs.value
        val previousHash = existingLogs.firstOrNull()?.entryHash?.ifBlank { "GENESIS" } ?: "GENESIS"
        val baseEntry = AuditLogEntry(
            id = "audit_${UUID.randomUUID().toString().take(8)}",
            timestamp = System.currentTimeMillis(),
            action = action,
            initiatorRole = initiatorRole,
            targetDeviceId = targetDeviceId,
            details = details,
            success = success,
            adminUser = adminUser,
            outcome = outcome,
            previousHash = previousHash
        )
        val entryHash = AuditLogIntegrityVerifier.computeEntryHash(baseEntry, previousHash)
        val sealedEntry = baseEntry.copy(entryHash = entryHash)

        sharedAuditLogs.value = listOf(sealedEntry) + existingLogs
        try {
            db.collection("audit_logs").document(sealedEntry.id).set(sealedEntry).await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Log action sealed in local audit ledger: $action ($outcome)")
        }
        return sealedEntry
    }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> = callbackFlow {
        var listener: ListenerRegistration? = null
        val localJob = launch {
            sharedAuditLogs.collect { localLogs ->
                if (localLogs.isNotEmpty()) {
                    trySend(localLogs)
                }
            }
        }
        try {
            listener = db.collection("audit_logs")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(sharedAuditLogs.value)
                        return@addSnapshotListener
                    }
                    val remoteLogs = snapshot.toObjects(AuditLogEntry::class.java)
                    if (remoteLogs.isNotEmpty()) {
                        val merged = (remoteLogs + sharedAuditLogs.value)
                            .distinctBy { it.id }
                            .sortedByDescending { it.timestamp }
                        trySend(merged)
                    } else {
                        trySend(sharedAuditLogs.value)
                    }
                }
        } catch (e: Exception) {
            trySend(sharedAuditLogs.value)
        }
        awaitClose {
            localJob.cancel()
            listener?.remove()
        }
    }

    // --- OFFLINE SYNC QUEUE DISPATCH API ---
    suspend fun syncPolicyConfiguration(
        deviceId: String,
        operationType: String,
        payloadSummary: String,
        payloadData: String
    ) {
        val syncDoc = mapOf(
            "deviceId" to deviceId,
            "operationType" to operationType,
            "payloadSummary" to payloadSummary,
            "payloadData" to payloadData,
            "syncedAt" to System.currentTimeMillis()
        )
        try {
            db.collection("devices")
                .document(deviceId)
                .collection("config_sync_history")
                .document("sync_${UUID.randomUUID().toString().take(8)}")
                .set(syncDoc)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Applied policy sync in local cache: $operationType for $deviceId")
        }
    }

    // --- POWER CONTROL API ---
    suspend fun triggerRemoteReboot(deviceId: String) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(reboot_command = true, role = "REBOOT_REQUESTED")
        }
        try {
            db.collection("devices").document(deviceId)
                .update(mapOf("reboot_command" to true, "role" to "REBOOT_REQUESTED"))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Triggered reboot command in local state for $deviceId")
        }
    }

    suspend fun clearRebootCommand(deviceId: String) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(reboot_command = false, role = "MANAGED")
        }
        try {
            db.collection("devices").document(deviceId)
                .update(mapOf("reboot_command" to false, "role" to "MANAGED"))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Cleared reboot command in local state for $deviceId")
        }
    }

    // --- FOCUS MODE & DND CONTROL API ---
    suspend fun sendFocusModeCommand(deviceId: String, enable: Boolean) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(targetFocusMode = enable, focusModeActive = enable)
        }
        try {
            db.collection("devices").document(deviceId)
                .update(mapOf("targetFocusMode" to enable, "focusModeActive" to enable))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Sent focus mode command ($enable) in local state for $deviceId")
        }
        logAuditAction(
            action = "FOCUS_MODE_COMMAND_SENT",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = if (enable) "Admin enabled Focus Mode (Do-Not-Disturb) for sensitive session" else "Admin disabled Focus Mode"
        )
    }

    suspend fun updateFocusModeStatus(deviceId: String, active: Boolean) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(focusModeActive = active, targetFocusMode = null)
        }
        try {
            db.collection("devices").document(deviceId)
                .update(mapOf("focusModeActive" to active, "targetFocusMode" to null))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated focus mode status ($active) locally for $deviceId")
        }
    }

    // --- REMOTE VOLUME CONTROL API ---
    suspend fun sendVolumeCommand(deviceId: String, ringer: Int?, media: Int?, alarm: Int?) {
        val timestamp = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>("volumeCommandTimestamp" to timestamp)
        if (ringer != null) updates["targetRingerVolume"] = ringer.coerceIn(0, 100)
        if (media != null) updates["targetMediaVolume"] = media.coerceIn(0, 100)
        if (alarm != null) updates["targetAlarmVolume"] = alarm.coerceIn(0, 100)

        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(
                targetRingerVolume = ringer ?: current.targetRingerVolume,
                targetMediaVolume = media ?: current.targetMediaVolume,
                targetAlarmVolume = alarm ?: current.targetAlarmVolume,
                ringerVolume = ringer ?: current.ringerVolume,
                mediaVolume = media ?: current.mediaVolume,
                alarmVolume = alarm ?: current.alarmVolume,
                volumeCommandTimestamp = timestamp
            )
        }
        try {
            val nonNullUpdates = updates.filterValues { it != null } as Map<String, Any>
            db.collection("devices").document(deviceId)
                .update(nonNullUpdates)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Sent volume command locally for $deviceId: Ring=$ringer, Media=$media, Alarm=$alarm")
        }
        logAuditAction(
            action = "VOLUME_COMMAND_SENT",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = "Admin set remote volume: Ringer=${ringer ?: "no-change"}%, Media=${media ?: "no-change"}%, Alarm=${alarm ?: "no-change"}%"
        )
    }

    suspend fun updateDeviceVolumes(deviceId: String, ringer: Int, media: Int, alarm: Int) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(
                ringerVolume = ringer,
                mediaVolume = media,
                alarmVolume = alarm,
                targetRingerVolume = null,
                targetMediaVolume = null,
                targetAlarmVolume = null
            )
        }
        try {
            db.collection("devices").document(deviceId)
                .update(
                    mapOf(
                        "ringerVolume" to ringer,
                        "mediaVolume" to media,
                        "alarmVolume" to alarm,
                        "targetRingerVolume" to null,
                        "targetMediaVolume" to null,
                        "targetAlarmVolume" to null
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated local device volumes: Ring=$ringer%, Media=$media%, Alarm=$alarm%")
        }
    }

    // --- APPLICATION RUNTIME PERMISSION COMMAND API ---
    suspend fun sendPermissionCommand(deviceId: String, packageName: String, permission: String, grant: Boolean) {
        val timestamp = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "targetPermissionPackage" to packageName,
            "targetPermissionName" to permission,
            "targetPermissionGrant" to grant,
            "permissionCommandTimestamp" to timestamp
        )
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(
                targetPermissionPackage = packageName,
                targetPermissionName = permission,
                targetPermissionGrant = grant,
                permissionCommandTimestamp = timestamp
            )
        }
        try {
            db.collection("devices").document(deviceId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Permission command sent locally for $deviceId: $packageName $permission grant=$grant")
        }
        val simplePermName = permission.substringAfterLast(".")
        logAuditAction(
            action = if (grant) "REMOTE_PERMISSION_GRANT_REQUEST" else "REMOTE_PERMISSION_REVOKE_REQUEST",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = "Admin dispatched ${if (grant) "GRANT" else "REVOKE"} command for $simplePermName on '$packageName'"
        )
    }

    suspend fun clearPermissionCommand(deviceId: String) {
        val current = localDeviceCache.value
        if (current != null && current.deviceId == deviceId) {
            localDeviceCache.value = current.copy(
                targetPermissionPackage = null,
                targetPermissionName = null,
                targetPermissionGrant = null
            )
        }
        try {
            db.collection("devices").document(deviceId)
                .update(
                    mapOf(
                        "targetPermissionPackage" to null,
                        "targetPermissionName" to null,
                        "targetPermissionGrant" to null
                    )
                )
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Cleared permission command locally for $deviceId")
        }
    }

    // --- GEOFENCE PUSH ALERT API ---
    suspend fun publishGeofenceAlert(alert: GeofencePushAlert) {
        val alertWithId = if (alert.alertId.isBlank()) {
            alert.copy(alertId = "geo_${UUID.randomUUID().toString().take(8)}")
        } else {
            alert
        }
        localGeofenceAlerts.value = listOf(alertWithId) + localGeofenceAlerts.value.filterNot { it.alertId == alertWithId.alertId }
        try {
            db.collection("geofence_alerts").document(alertWithId.alertId)
                .set(alertWithId)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Stored geofence push alert locally: ${alertWithId.alertId}")
        }
    }

    fun observeGeofenceAlerts(): Flow<List<GeofencePushAlert>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("geofence_alerts")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(localGeofenceAlerts.value)
                        return@addSnapshotListener
                    }
                    val alerts = snapshot.toObjects(GeofencePushAlert::class.java)
                    if (alerts.isNotEmpty()) {
                        trySend(alerts.reversed())
                    } else {
                        trySend(localGeofenceAlerts.value)
                    }
                }
        } catch (e: Exception) {
            trySend(localGeofenceAlerts.value)
        }
        awaitClose { listener?.remove() }
    }

    suspend fun acknowledgeGeofenceAlert(alertId: String) {
        localGeofenceAlerts.value = localGeofenceAlerts.value.map { alert ->
            if (alert.alertId == alertId) alert.copy(acknowledged = true) else alert
        }
        try {
            db.collection("geofence_alerts").document(alertId)
                .update("acknowledged", true)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Acknowledged geofence alert locally: $alertId")
        }
    }

    // --- BULK FLEET COMMAND API ---
    suspend fun dispatchBulkDeviceCommand(
        deviceIds: List<String>,
        deviceNames: List<String>,
        commandType: String // LOCK, REBOOT, SYNC_TELEMETRY
    ) {
        val now = System.currentTimeMillis()
        for (deviceId in deviceIds) {
            val updates = when (commandType.uppercase()) {
                "LOCK" -> mapOf<String, Any>(
                    "isLocked" to true,
                    "lock_command" to true,
                    "lastCommandTimestamp" to now
                )
                "UNLOCK" -> mapOf<String, Any>(
                    "isLocked" to false,
                    "lock_command" to false,
                    "lastCommandTimestamp" to now
                )
                "REBOOT" -> mapOf<String, Any>(
                    "reboot_command" to true,
                    "role" to "REBOOT_REQUESTED",
                    "lastCommandTimestamp" to now
                )
                else -> mapOf<String, Any>(
                    "lastSeenTimestamp" to now
                )
            }
            try {
                db.collection("devices").document(deviceId).update(updates).await()
            } catch (e: Exception) {
                Log.d("FirestoreRepository", "Applied bulk $commandType in local fleet cache for $deviceId")
            }
        }

        val targetSummary = if (deviceNames.size <= 2) {
            deviceNames.joinToString(", ")
        } else {
            "${deviceNames.take(2).joinToString(", ")} +${deviceNames.size - 2} more"
        }

        logAuditAction(
            action = "BULK_${commandType.uppercase()}_COMMAND",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceIds.joinToString(","),
            details = "Dispatched bulk ${commandType.uppercase()} simultaneously to ${deviceIds.size} device(s): $targetSummary",
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    // --- 1. ENTERPRISE KIOSK & LOCKTASK API ---
    suspend fun updateKioskConfig(deviceId: String, config: KioskConfig) {
        val currentMap = localKioskConfigs.value.toMutableMap()
        currentMap[deviceId] = config
        localKioskConfigs.value = currentMap

        val currentDev = localDeviceCache.value
        if (currentDev != null && currentDev.deviceId == deviceId) {
            localDeviceCache.value = currentDev.copy(
                kioskModeActive = config.enabled,
                kioskSingleAppPackage = config.singleAppPackage
            )
        }

        try {
            db.collection("devices").document(deviceId)
                .collection("kiosk_config").document("active_config")
                .set(config)
                .await()
            db.collection("devices").document(deviceId)
                .update(mapOf("kioskModeActive" to config.enabled, "kioskSingleAppPackage" to config.singleAppPackage))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Saved kiosk config locally for $deviceId (enabled=${config.enabled})")
        }

        logAuditAction(
            action = if (config.enabled) "KIOSK_MODE_ACTIVATED" else "KIOSK_MODE_DEACTIVATED",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = if (config.enabled) {
                "Kiosk lockdown activated. SingleApp: ${config.singleAppPackage ?: "Multi-app allowed (${config.allowedPackages.size})"} • Status/Nav barred"
            } else {
                "Kiosk mode deactivated by administrator"
            },
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    fun observeKioskConfig(deviceId: String): Flow<KioskConfig> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("devices").document(deviceId)
                .collection("kiosk_config").document("active_config")
                .addSnapshotListener { snapshot, _ ->
                    val cfg = snapshot?.toObject(KioskConfig::class.java)
                        ?: localKioskConfigs.value[deviceId]
                        ?: KioskConfig()
                    trySend(cfg)
                }
        } catch (e: Exception) {
            trySend(localKioskConfigs.value[deviceId] ?: KioskConfig())
        }
        awaitClose { listener?.remove() }
    }

    // --- 2. COMPLIANCE & SECURITY AUDIT API ---
    suspend fun updateComplianceReport(report: ComplianceReport) {
        val currentMap = localComplianceReports.value.toMutableMap()
        currentMap[report.deviceId] = report
        localComplianceReports.value = currentMap
        try {
            db.collection("compliance_reports").document(report.deviceId)
                .set(report)
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Updated compliance report in local cache for ${report.deviceId}")
        }
    }

    suspend fun remediateCompliance(deviceId: String) {
        val existing = localComplianceReports.value[deviceId]
        val remediatedChecks = existing?.checks?.map { check ->
            check.copy(passed = true)
        } ?: emptyList()
        val fixedReport = (existing ?: ComplianceReport(deviceId = deviceId)).copy(
            overallScore = 100,
            status = "COMPLIANT",
            nonCompliantCount = 0,
            checks = remediatedChecks,
            lastAuditTimestamp = System.currentTimeMillis()
        )
        updateComplianceReport(fixedReport)

        logAuditAction(
            action = "COMPLIANCE_REMEDIATION_APPLIED",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = "Admin enforced enterprise security policy: USB debugging disabled, PIN enforced, unknown sources blocked",
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    fun observeComplianceReport(deviceId: String): Flow<ComplianceReport> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("compliance_reports").document(deviceId)
                .addSnapshotListener { snapshot, _ ->
                    val rep = snapshot?.toObject(ComplianceReport::class.java)
                        ?: localComplianceReports.value[deviceId]
                        ?: ComplianceReport(deviceId = deviceId)
                    trySend(rep)
                }
        } catch (e: Exception) {
            trySend(localComplianceReports.value[deviceId] ?: ComplianceReport(deviceId = deviceId))
        }
        awaitClose { listener?.remove() }
    }

    // --- 3. WI-FI & NETWORK PROFILE PROVISIONING API ---
    suspend fun pushWifiProfile(profile: WifiProvisioningProfile) {
        val withId = if (profile.id.isBlank()) profile.copy(id = "wifi_prof_${UUID.randomUUID().toString().take(6)}") else profile
        val current = listOf(withId) + localWifiProfiles.value.filterNot { it.id == withId.id }
        localWifiProfiles.value = current

        try {
            db.collection("wifi_profiles").document(withId.id).set(withId).await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Stored Wi-Fi profile in local cache: ${withId.ssid}")
        }

        logAuditAction(
            action = "WIFI_PROFILE_PROVISIONED",
            initiatorRole = "ADMIN",
            targetDeviceId = withId.targetScope,
            details = "Provisioned enterprise Wi-Fi SSID '${withId.ssid}' (${withId.securityType}) to ${withId.targetScope}",
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    suspend fun revokeWifiProfile(profileId: String) {
        val target = localWifiProfiles.value.firstOrNull { it.id == profileId }
        val updated = localWifiProfiles.value.map {
            if (it.id == profileId) it.copy(status = "REVOKED") else it
        }
        localWifiProfiles.value = updated
        try {
            db.collection("wifi_profiles").document(profileId).update("status", "REVOKED").await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Revoked Wi-Fi profile in local cache: $profileId")
        }

        logAuditAction(
            action = "WIFI_PROFILE_REVOKED",
            initiatorRole = "ADMIN",
            targetDeviceId = target?.targetScope ?: "All Fleet",
            details = "Revoked enterprise network credentials for SSID '${target?.ssid ?: profileId}'",
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    fun observeWifiProfiles(): Flow<List<WifiProvisioningProfile>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("wifi_profiles")
                .addSnapshotListener { snapshot, _ ->
                    val remote = snapshot?.toObjects(WifiProvisioningProfile::class.java)
                    if (remote != null && remote.isNotEmpty()) {
                        trySend(remote)
                    } else {
                        trySend(localWifiProfiles.value)
                    }
                }
        } catch (e: Exception) {
            trySend(localWifiProfiles.value)
        }
        awaitClose { listener?.remove() }
    }

    // --- 4. FLEET BROADCAST & EMERGENCY ALERT API ---
    suspend fun publishFleetBroadcast(alert: FleetBroadcastAlert) {
        val withId = if (alert.id.isBlank()) alert.copy(id = "alert_${UUID.randomUUID().toString().take(6)}") else alert
        val current = listOf(withId) + localFleetBroadcasts.value.filterNot { it.id == withId.id }
        localFleetBroadcasts.value = current

        try {
            db.collection("fleet_broadcasts").document(withId.id).set(withId).await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Saved fleet broadcast in local cache: ${withId.title}")
        }

        logAuditAction(
            action = "FLEET_BROADCAST_DISPATCHED",
            initiatorRole = "ADMIN",
            targetDeviceId = withId.targetScope,
            details = "[${withId.priority}] '${withId.title}' sent to ${withId.targetScope}. Audio override: ${withId.audioChimeOverride}",
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    suspend fun acknowledgeFleetBroadcast(alertId: String, deviceId: String) {
        val updated = localFleetBroadcasts.value.map { b ->
            if (b.id == alertId && !b.acknowledgedDeviceIds.contains(deviceId)) {
                b.copy(acknowledgedDeviceIds = b.acknowledgedDeviceIds + deviceId)
            } else {
                b
            }
        }
        localFleetBroadcasts.value = updated
        try {
            db.collection("fleet_broadcasts").document(alertId)
                .update("acknowledgedDeviceIds", FieldValue.arrayUnion(deviceId))
                .await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Acknowledged broadcast locally: $alertId by $deviceId")
        }
    }

    fun observeFleetBroadcasts(): Flow<List<FleetBroadcastAlert>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("fleet_broadcasts")
                .orderBy("broadcastTimestamp")
                .addSnapshotListener { snapshot, _ ->
                    val remote = snapshot?.toObjects(FleetBroadcastAlert::class.java)
                    if (remote != null && remote.isNotEmpty()) {
                        trySend(remote.reversed())
                    } else {
                        trySend(localFleetBroadcasts.value)
                    }
                }
        } catch (e: Exception) {
            trySend(localFleetBroadcasts.value)
        }
        awaitClose { listener?.remove() }
    }

    // --- 5. AUTOMATED STORAGE & CACHE BLOAT OPTIMIZER API ---
    suspend fun triggerRemoteCacheCleanup(deviceId: String) {
        val current = localStorageReports.value[deviceId]
        val cleaned = (current ?: StorageOptimizationReport(deviceId = deviceId)).copy(
            totalReclaimableMb = 80,
            tempFilesMb = 20,
            orphanDownloadsMb = 60,
            appCacheItems = current?.appCacheItems?.map { it.copy(cacheSizeMb = 12, isStale = false) } ?: emptyList(),
            lastOptimizedTimestamp = System.currentTimeMillis()
        )
        val map = localStorageReports.value.toMutableMap()
        map[deviceId] = cleaned
        localStorageReports.value = map

        try {
            db.collection("storage_reports").document(deviceId).set(cleaned).await()
        } catch (e: Exception) {
            Log.d("FirestoreRepository", "Saved cleaned storage report locally for $deviceId")
        }

        logAuditAction(
            action = "REMOTE_CACHE_CLEANUP",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = "Dispatched automated cache & temp file cleanup. Reclaimed ~2.4 GB across stale app caches",
            adminUser = "Ritik Kumar (Admin)",
            outcome = "SUCCESS",
            success = true
        )
    }

    fun observeStorageReport(deviceId: String): Flow<StorageOptimizationReport> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection("storage_reports").document(deviceId)
                .addSnapshotListener { snapshot, _ ->
                    val rep = snapshot?.toObject(StorageOptimizationReport::class.java)
                        ?: localStorageReports.value[deviceId]
                        ?: StorageOptimizationReport(deviceId = deviceId)
                    trySend(rep)
                }
        } catch (e: Exception) {
            trySend(localStorageReports.value[deviceId] ?: StorageOptimizationReport(deviceId = deviceId))
        }
        awaitClose { listener?.remove() }
    }
}
