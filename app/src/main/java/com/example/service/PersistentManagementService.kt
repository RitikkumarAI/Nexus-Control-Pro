package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.LocalDeviceManager
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PersistentManagementService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var telemetryJob: Job? = null
    private lateinit var localDeviceManager: LocalDeviceManager
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        private const val CHANNEL_ID = "nexus_persistent_service_channel"
        private const val NOTIFICATION_ID = 8801

        fun startService(context: Context) {
            val intent = Intent(context, PersistentManagementService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PersistentManagementService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        localDeviceManager = LocalDeviceManager(applicationContext)
        firestoreRepository = FirestoreRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)

        createNotificationChannel()
        startForegroundWithType()
        startTelemetryLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithType()
                startTelemetryLoop()
            }
        }
        // Return START_STICKY so Android system restarts service if terminated
        return START_STICKY
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

    private fun startTelemetryLoop() {
        if (telemetryJob?.isActive == true) return
        telemetryJob = serviceScope.launch {
            while (isActive) {
                try {
                    val role = preferencesManager.roleFlow.firstOrNull()?.name ?: "MANAGED"
                    val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
                    val telemetry = localDeviceManager.getLocalDeviceTelemetry(
                        deviceId = deviceId,
                        deviceName = if (role == "ADMIN") "Phone B (Admin)" else "Phone B (Managed)",
                        role = role
                    )
                    firestoreRepository.updateDeviceTelemetry(telemetry)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30000L) // Refresh telemetry every 30 seconds
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nexus Remote Management Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains 24/7 background telemetry sync for remote management"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS CONTROL PRO Active")
            .setContentText("24/7 background connectivity active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        telemetryJob?.cancel()
        super.onDestroy()
    }
}
