package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.PreferencesManager
import com.example.data.local.AppUsageEntity
import com.example.data.local.NexusDatabase
import com.example.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppUsageTrackerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var trackingJob: Job? = null

    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var packageManagerRef: PackageManager
    private var usageStatsManager: UsageStatsManager? = null

    companion object {
        const val ACTION_START = "com.example.service.START_APP_TRACKER"
        const val ACTION_STOP = "com.example.service.STOP_APP_TRACKER"
        private const val CHANNEL_ID = "nexus_app_usage_channel"
        private const val NOTIFICATION_ID = 8805

        fun startService(context: Context) {
            val intent = Intent(context, AppUsageTrackerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AppUsageTrackerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        firestoreRepository = FirestoreRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        packageManagerRef = applicationContext.packageManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        createNotificationChannel()
        startForegroundWithType()
        startAppTrackingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                trackingJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithType()
                startAppTrackingLoop()
            }
        }
        return START_STICKY
    }

    private fun startAppTrackingLoop() {
        if (trackingJob?.isActive == true) return
        trackingJob = serviceScope.launch {
            while (isActive) {
                try {
                    val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
                    trackAndPersistAppUsage(deviceId)
                } catch (e: Exception) {
                    Log.e("AppUsageTrackerService", "Tracking error: ${e.message}")
                }
                delay(60000L) // Poll every 60 seconds
            }
        }
    }

    private suspend fun trackAndPersistAppUsage(deviceId: String) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000L)

        val usageStatsList: List<UsageStats> = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val roomEntities = mutableListOf<AppUsageEntity>()
        val appItems = mutableListOf<AppItem>()

        for (stats in usageStatsList) {
            if (stats.totalTimeInForeground <= 0) continue
            val pkg = stats.packageName

            try {
                val appInfo = packageManagerRef.getApplicationInfo(pkg, 0)
                val label = packageManagerRef.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                roomEntities.add(
                    AppUsageEntity(
                        packageName = pkg,
                        appName = label,
                        usageTimeMs = stats.totalTimeInForeground,
                        lastTimeUsed = stats.lastTimeUsed,
                        isSystemApp = isSystem
                    )
                )

                appItems.add(
                    AppItem(
                        packageName = pkg,
                        label = label,
                        versionName = "1.0",
                        versionCode = 1L,
                        isSystemApp = isSystem,
                        installTime = stats.lastTimeUsed,
                        usageTimeMillisMs = stats.totalTimeInForeground
                    )
                )
            } catch (e: Exception) {
                continue
            }
        }

        if (roomEntities.isNotEmpty()) {
            // 1. Save to Room Local Database
            NexusDatabase.getDatabase(applicationContext).appUsageDao().insertAppUsages(roomEntities)
            Log.d("AppUsageTrackerService", "Saved ${roomEntities.size} app usage entries to Room")

            // 2. Sync to Firestore for administrative insights
            firestoreRepository.logAuditAction(
                action = "APP_USAGE_SYNC",
                initiatorRole = "SYSTEM",
                targetDeviceId = deviceId,
                details = "Synced ${roomEntities.size} active app usage metrics"
            )
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
                "Nexus App Usage Tracker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors installed application usage metrics for administrative insights"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS App Usage Tracker")
            .setContentText("Syncing app activity metrics for administrative insights")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        trackingJob?.cancel()
        super.onDestroy()
    }
}
