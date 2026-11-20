package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.WebRTCManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ConnectivityForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var webRTCManager: WebRTCManager

    companion object {
        const val ACTION_START_CONNECTIVITY = "com.example.service.START_CONNECTIVITY"
        const val ACTION_STOP_CONNECTIVITY = "com.example.service.STOP_CONNECTIVITY"
        private const val CHANNEL_ID = "nexus_connectivity_channel"
        private const val NOTIFICATION_ID = 8802

        fun startService(context: Context) {
            val intent = Intent(context, ConnectivityForegroundService::class.java).apply {
                action = ACTION_START_CONNECTIVITY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ConnectivityForegroundService::class.java).apply {
                action = ACTION_STOP_CONNECTIVITY
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        firestoreRepository = FirestoreRepository(applicationContext)
        webRTCManager = WebRTCManager(applicationContext, firestoreRepository)

        createNotificationChannel()
        startForegroundWithType()
        startConnectivityLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_CONNECTIVITY -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_CONNECTIVITY, null -> {
                startForegroundWithType()
                startConnectivityLoop()
            }
        }
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

    private fun startConnectivityLoop() {
        if (syncJob?.isActive == true) return
        syncJob = serviceScope.launch {
            Log.d("ConnectivityService", "24/7 Firebase Firestore and WebRTC signaling loop active.")
            while (isActive) {
                val delayMs = calculateSyncInterval()
                Log.d("ConnectivityService", "Sync loop heartening with interval=${delayMs}ms")
                try {
                    delay(delayMs)
                } catch (e: Exception) {
                    Log.e("ConnectivityService", "Heartbeat error: ${e.message}")
                }
            }
        }
    }

    private fun calculateSyncInterval(): Long {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val pct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Dynamically adjust sync frequency to conserve battery when level < 20%
        return if (!isCharging && pct < 20) {
            60000L // Low-frequency battery saver mode (60s)
        } else {
            15000L // Normal sync mode (15s)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nexus Live Connectivity Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains 24/7 background Firebase and WebRTC signal stream"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS Connectivity")
            .setContentText("24/7 WebRTC & Firebase signaling background stream active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        syncJob?.cancel()
        webRTCManager.close()
        super.onDestroy()
    }
}
