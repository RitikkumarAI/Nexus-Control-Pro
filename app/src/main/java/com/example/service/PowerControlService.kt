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
import com.example.receiver.NexusDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PowerControlService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var observeJob: Job? = null
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    companion object {
        const val ACTION_START_POWER_CONTROL = "com.example.service.START_POWER_CONTROL"
        const val ACTION_STOP_POWER_CONTROL = "com.example.service.STOP_POWER_CONTROL"
        private const val CHANNEL_ID = "nexus_power_control_channel"
        private const val NOTIFICATION_ID = 8805

        fun startService(context: Context) {
            val intent = Intent(context, PowerControlService::class.java).apply {
                action = ACTION_START_POWER_CONTROL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PowerControlService::class.java).apply {
                action = ACTION_STOP_POWER_CONTROL
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
        startRebootListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_POWER_CONTROL -> {
                observeJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_POWER_CONTROL, null -> {
                startForegroundWithType()
            }
        }
        return START_STICKY
    }

    private fun startRebootListener() {
        if (observeJob?.isActive == true) return
        observeJob = serviceScope.launch {
            val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
            Log.d("PowerControlService", "Listening for reboot_command in Firestore for device $deviceId")

            firestoreRepository.observeDevice(deviceId).collect { device ->
                if (device == null || !isActive) return@collect

                val isRebootRequested = device.reboot_command || device.role == "REBOOT_REQUESTED"

                if (isRebootRequested) {
                    Log.w("PowerControlService", "REBOOT COMMAND RECEIVED: Initiating remote device restart")

                    // Clear reboot command flag in Firestore to prevent continuous reboot loops
                    firestoreRepository.clearRebootCommand(deviceId)

                    // Audit Log entry
                    firestoreRepository.logAuditAction(
                        action = "REMOTE_REBOOT",
                        initiatorRole = "ADMIN",
                        targetDeviceId = deviceId,
                        details = "Remote restart triggered via Firestore 'reboot_command'. Executing ProcessBuilder ('svc power reboot')."
                    )

                    // Execute reboot sequence
                    executePowerReboot()
                }
            }
        }
    }

    private fun executePowerReboot() {
        Log.w("PowerControlService", "Executing ProcessBuilder 'svc power reboot'")

        try {
            // Primary path: ProcessBuilder executing 'svc power reboot'
            val pb = ProcessBuilder("svc", "power", "reboot")
            pb.redirectErrorStream(true)
            val process = pb.start()
            val exitCode = process.waitFor()
            Log.i("PowerControlService", "ProcessBuilder 'svc power reboot' exited with code $exitCode")

            if (exitCode != 0) {
                // Root fallback if available
                val rootPb = ProcessBuilder("su", "-c", "svc power reboot")
                rootPb.start()
            }
        } catch (e: Exception) {
            Log.e("PowerControlService", "ProcessBuilder reboot error: ${e.message}")
        }

        // Secondary path: DevicePolicyManager reboot on API 24+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && devicePolicyManager.isAdminActive(adminComponent)) {
                Log.i("PowerControlService", "Invoking DevicePolicyManager.reboot(adminComponent)")
                devicePolicyManager.reboot(adminComponent)
            }
        } catch (e: Exception) {
            Log.e("PowerControlService", "DevicePolicyManager reboot exception: ${e.message}")
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
                "Nexus Remote Power Control Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors remote power commands and restart requests"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS Power Control")
            .setContentText("Monitoring remote restart commands")
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
