package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusModeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var observeJob: Job? = null
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager

    private var lastProcessedVolumeTimestamp: Long = 0L
    private var lastProcessedFocusTarget: Boolean? = null

    companion object {
        const val ACTION_START_FOCUS_MODE = "com.example.service.START_FOCUS_MODE"
        const val ACTION_STOP_FOCUS_MODE = "com.example.service.STOP_FOCUS_MODE"
        private const val CHANNEL_ID = "nexus_focus_mode_channel"
        private const val NOTIFICATION_ID = 8808

        fun startService(context: Context) {
            val intent = Intent(context, FocusModeService::class.java).apply {
                action = ACTION_START_FOCUS_MODE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusModeService::class.java).apply {
                action = ACTION_STOP_FOCUS_MODE
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        firestoreRepository = FirestoreRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        startForegroundWithType()
        startCommandListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_FOCUS_MODE -> {
                observeJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_FOCUS_MODE, null -> {
                startForegroundWithType()
            }
        }
        return START_STICKY
    }

    private fun startCommandListener() {
        if (observeJob?.isActive == true) return
        observeJob = serviceScope.launch {
            val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
            Log.d("FocusModeService", "Listening for Focus Mode & Volume commands for device $deviceId")

            firestoreRepository.observeDevice(deviceId).collect { device ->
                if (device == null || !isActive) return@collect

                // 1. Process Focus Mode (DND) command from Admin
                if (device.targetFocusMode != null && device.targetFocusMode != lastProcessedFocusTarget) {
                    val enable = device.targetFocusMode
                    lastProcessedFocusTarget = enable
                    Log.i("FocusModeService", "Received Focus Mode command from Admin: enable=$enable")
                    applyFocusMode(enable, deviceId)
                }

                // 2. Process Remote Volume command from Admin
                val hasVolumeTargets = device.targetRingerVolume != null ||
                        device.targetMediaVolume != null ||
                        device.targetAlarmVolume != null

                val cmdTime = device.volumeCommandTimestamp ?: 0L
                if (hasVolumeTargets && cmdTime > lastProcessedVolumeTimestamp) {
                    lastProcessedVolumeTimestamp = cmdTime
                    Log.i(
                        "FocusModeService",
                        "Received Remote Volume command from Admin: Ringer=${device.targetRingerVolume}, Media=${device.targetMediaVolume}, Alarm=${device.targetAlarmVolume}"
                    )
                    applyVolumeCommand(
                        ringer = device.targetRingerVolume,
                        media = device.targetMediaVolume,
                        alarm = device.targetAlarmVolume,
                        deviceId = deviceId
                    )
                }
            }
        }
    }

    /**
     * Toggles system Do-Not-Disturb status via NotificationManager.
     * Silences all notifications on the managed device during sensitive remote sessions.
     */
    private fun applyFocusMode(enabled: Boolean, deviceId: String) {
        try {
            var dndApplied = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    val filter = if (enabled) {
                        NotificationManager.INTERRUPTION_FILTER_NONE
                    } else {
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    }
                    notificationManager.setInterruptionFilter(filter)
                    dndApplied = true
                    Log.i("FocusModeService", "NotificationManager interruption filter set to $filter")
                } else {
                    Log.w("FocusModeService", "Notification Policy access not granted; applying audio silence fallback")
                }
            }

            // Also synchronize AudioManager ringer mode
            try {
                if (enabled) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                }
            } catch (e: Exception) {
                Log.w("FocusModeService", "Ringer mode adjustment error: ${e.message}")
            }

            // Sync verified status to Firestore and write Audit log
            serviceScope.launch {
                firestoreRepository.updateFocusModeStatus(deviceId, enabled)
                firestoreRepository.logAuditAction(
                    action = "FOCUS_MODE_EXECUTED",
                    initiatorRole = "ADMIN",
                    targetDeviceId = deviceId,
                    details = if (enabled) {
                        "Focus Mode (Do-Not-Disturb) ACTIVATED: Notifications silenced via NotificationManager (Filter: NONE)"
                    } else {
                        "Focus Mode (Do-Not-Disturb) DEACTIVATED: Normal notifications restored via NotificationManager (Filter: ALL)"
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("FocusModeService", "Error toggling Focus Mode DND: ${e.message}")
        }
    }

    /**
     * Adjusts the device's ringer, media, and alarm volumes remotely via AudioManager.
     */
    private fun applyVolumeCommand(ringer: Int?, media: Int?, alarm: Int?, deviceId: String) {
        try {
            // 1. Ringer Volume
            if (ringer != null) {
                val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                val targetRing = (ringer.coerceIn(0, 100) * maxRing) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_RING, targetRing, 0)
                Log.d("FocusModeService", "Set STREAM_RING to $targetRing (max $maxRing)")
            }

            // 2. Media Volume
            if (media != null) {
                val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetMedia = (media.coerceIn(0, 100) * maxMedia) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetMedia, 0)
                Log.d("FocusModeService", "Set STREAM_MUSIC to $targetMedia (max $maxMedia)")
            }

            // 3. Alarm Volume
            if (alarm != null) {
                val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetAlarm = (alarm.coerceIn(0, 100) * maxAlarm) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetAlarm, 0)
                Log.d("FocusModeService", "Set STREAM_ALARM to $targetAlarm (max $maxAlarm)")
            }

            // Read back actual values
            val actualRing = getStreamPercent(AudioManager.STREAM_RING)
            val actualMedia = getStreamPercent(AudioManager.STREAM_MUSIC)
            val actualAlarm = getStreamPercent(AudioManager.STREAM_ALARM)

            serviceScope.launch {
                firestoreRepository.updateDeviceVolumes(deviceId, actualRing, actualMedia, actualAlarm)
                firestoreRepository.logAuditAction(
                    action = "REMOTE_VOLUME_EXECUTED",
                    initiatorRole = "ADMIN",
                    targetDeviceId = deviceId,
                    details = "AudioManager applied remote volume: Ringer=$actualRing%, Media=$actualMedia%, Alarm=$actualAlarm%"
                )
            }
        } catch (e: Exception) {
            Log.e("FocusModeService", "Error applying remote volume adjustments: ${e.message}")
        }
    }

    private fun getStreamPercent(streamType: Int): Int {
        return try {
            val max = audioManager.getStreamMaxVolume(streamType)
            if (max <= 0) return 0
            val cur = audioManager.getStreamVolume(streamType)
            ((cur.toFloat() / max) * 100).toInt()
        } catch (e: Exception) {
            50
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
                "Nexus Remote Focus Mode & Volume Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors remote Do-Not-Disturb focus commands and audio adjustments"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS Focus Mode & Audio")
            .setContentText("Monitoring remote Do-Not-Disturb and volume telemetry")
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
