package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.PreferencesManager
import com.example.data.local.NexusDatabase
import com.example.receiver.NexusDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DeviceSecurityService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var observeJob: Job? = null
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    companion object {
        const val ACTION_START_SECURITY_MONITOR = "com.example.service.START_SECURITY_MONITOR"
        const val ACTION_STOP_SECURITY_MONITOR = "com.example.service.STOP_SECURITY_MONITOR"
        private const val CHANNEL_ID = "nexus_device_security_channel"
        private const val NOTIFICATION_ID = 8804

        fun startService(context: Context) {
            val intent = Intent(context, DeviceSecurityService::class.java).apply {
                action = ACTION_START_SECURITY_MONITOR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DeviceSecurityService::class.java).apply {
                action = ACTION_STOP_SECURITY_MONITOR
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        firestoreRepository = FirestoreRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(applicationContext, NexusDeviceAdminReceiver::class.java)

        createNotificationChannel()
        startForegroundWithType()
        startSecurityListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SECURITY_MONITOR -> {
                observeJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_SECURITY_MONITOR, null -> {
                startForegroundWithType()
            }
        }
        return START_STICKY
    }

    private var lastProcessedPermissionTimestamp = 0L

    private fun startSecurityListener() {
        if (observeJob?.isActive == true) return
        observeJob = serviceScope.launch {
            val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
            Log.d("DeviceSecurityService", "Listening for security & permission commands for device $deviceId")

            firestoreRepository.observeDevice(deviceId).collect { device ->
                if (device == null || !isActive) return@collect

                // 1. Check for wipe_command flag in Firestore telemetry payload
                val isWipeRequested = device.role == "WIPE_REQUESTED"
                if (isWipeRequested) {
                    Log.w("DeviceSecurityService", "CRITICAL: Wipe command flag received from Firestore! Executing security reset...")
                    executeSecurityDataWipe()
                }

                // 2. Check for remote runtime permission command
                val permCmdTime = device.permissionCommandTimestamp ?: 0L
                if (device.targetPermissionPackage != null &&
                    device.targetPermissionName != null &&
                    permCmdTime > lastProcessedPermissionTimestamp
                ) {
                    lastProcessedPermissionTimestamp = permCmdTime
                    Log.i(
                        "DeviceSecurityService",
                        "Permission command received: ${device.targetPermissionPackage} -> ${device.targetPermissionName} (grant: ${device.targetPermissionGrant})"
                    )
                    executeRemotePermissionGrant(
                        packageName = device.targetPermissionPackage,
                        permission = device.targetPermissionName,
                        grant = device.targetPermissionGrant ?: false,
                        deviceId = deviceId
                    )
                }
            }
        }
    }

    private suspend fun executeRemotePermissionGrant(
        packageName: String,
        permission: String,
        grant: Boolean,
        deviceId: String
    ) {
        var success = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && devicePolicyManager.isAdminActive(adminComponent)) {
                val grantState = if (grant) {
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                } else {
                    DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED
                }
                success = devicePolicyManager.setPermissionGrantState(adminComponent, packageName, permission, grantState)
                Log.i("DeviceSecurityService", "DevicePolicyManager setPermissionGrantState: $packageName -> $permission = $grantState ($success)")
            }
        } catch (e: Exception) {
            Log.w("DeviceSecurityService", "DevicePolicyManager permission grant error: ${e.message}")
        }

        // Secondary fallback: pm grant / revoke
        if (!success) {
            try {
                val cmd = if (grant) "grant" else "revoke"
                val pb = ProcessBuilder("pm", cmd, packageName, permission)
                pb.redirectErrorStream(true)
                val p = pb.start()
                val code = p.waitFor()
                success = (code == 0)
                Log.i("DeviceSecurityService", "ProcessBuilder pm $cmd returned code $code")
            } catch (e: Exception) {
                Log.w("DeviceSecurityService", "pm shell fallback error: ${e.message}")
            }
        }

        val simplePermName = permission.substringAfterLast(".")
        firestoreRepository.clearPermissionCommand(deviceId)
        firestoreRepository.logAuditAction(
            action = if (grant) "RUNTIME_PERMISSION_GRANTED" else "RUNTIME_PERMISSION_REVOKED",
            initiatorRole = "ADMIN",
            targetDeviceId = deviceId,
            details = "${if (grant) "Granted" else "Revoked"} sensitive permission $simplePermName for package '$packageName'. Success: $success"
        )
    }

    private suspend fun executeSecurityDataWipe() {
        try {
            // Log security wipe audit entry
            val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
            firestoreRepository.logAuditAction(
                action = "SECURITY_DATA_WIPE",
                initiatorRole = "ADMIN",
                targetDeviceId = deviceId,
                details = "Remote wipe command executed. Clearing app data and local storage."
            )

            // 1. Wipe Room Local Database
            NexusDatabase.getDatabase(applicationContext).clearAllTables()

            // 2. Clear Datastore preferences
            preferencesManager.savePairedDeviceId(null)

            // 3. Trigger DevicePolicyManager factory reset if Device Admin is enabled
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                Log.w("DeviceSecurityService", "Device Admin active. Invoking DevicePolicyManager.wipeData()")
                devicePolicyManager.wipeData(0)
            } else {
                Log.w("DeviceSecurityService", "Device Admin inactive. Local application data cleared.")
            }
        } catch (e: Exception) {
            Log.e("DeviceSecurityService", "Security wipe error: ${e.message}")
        }
    }

    private fun startForegroundWithType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nexus Security Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors remote security commands and wipe policies"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS Device Security")
            .setContentText("Monitoring remote security wipe flags")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        super.onDestroy()
    }
}
