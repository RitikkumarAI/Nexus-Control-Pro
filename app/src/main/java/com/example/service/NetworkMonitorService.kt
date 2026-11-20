package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NetworkMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val ACTION_START_NETWORK_MONITOR = "com.example.service.START_NETWORK_MONITOR"
        const val ACTION_STOP_NETWORK_MONITOR = "com.example.service.STOP_NETWORK_MONITOR"
        private const val CHANNEL_ID = "nexus_network_monitor_channel"
        private const val NOTIFICATION_ID = 8803

        fun startService(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_START_NETWORK_MONITOR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_STOP_NETWORK_MONITOR
            }
            context.startService(intent)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            syncConnectivityState(network)
        }

        override fun onLost(network: Network) {
            syncOfflineState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            syncConnectivityState(network, networkCapabilities)
        }
    }

    override fun onCreate() {
        super.onCreate()
        firestoreRepository = FirestoreRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        createNotificationChannel()
        startForegroundWithType()
        registerNetworkObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_NETWORK_MONITOR -> {
                unregisterNetworkObserver()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_NETWORK_MONITOR, null -> {
                startForegroundWithType()
            }
        }
        return START_STICKY
    }

    private fun registerNetworkObserver() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)

            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                syncConnectivityState(activeNetwork)
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitorService", "Failed to register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkObserver() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkMonitorService", "Failed to unregister network callback: ${e.message}")
        }
    }

    private fun syncConnectivityState(
        network: Network,
        capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
    ) {
        val caps = capabilities ?: return
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val networkTypeStr = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular 4G/5G"
            else -> "Ethernet"
        }

        serviceScope.launch {
            try {
                val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
                firestoreRepository.updateDeviceNetworkState(
                    deviceId = deviceId,
                    networkType = networkTypeStr,
                    isOnline = true
                )
                Log.d("NetworkMonitorService", "Synced network state: $networkTypeStr for device $deviceId")
            } catch (e: Exception) {
                Log.e("NetworkMonitorService", "Firestore sync error: ${e.message}")
            }
        }
    }

    private fun syncOfflineState() {
        serviceScope.launch {
            try {
                val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
                firestoreRepository.updateDeviceNetworkState(
                    deviceId = deviceId,
                    networkType = "Offline",
                    isOnline = false
                )
            } catch (e: Exception) {
                Log.e("NetworkMonitorService", "Firestore offline sync error: ${e.message}")
            }
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
                "Nexus Network Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors real-time Wi-Fi and mobile cellular data telemetry"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS Network Monitor")
            .setContentText("Tracking Wi-Fi and mobile data connection status")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterNetworkObserver()
        super.onDestroy()
    }
}
