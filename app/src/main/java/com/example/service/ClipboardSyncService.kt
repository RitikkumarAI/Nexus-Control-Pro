package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ClipboardSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var preferencesManager: PreferencesManager

    companion object {
        const val ACTION_START = "com.example.service.START_CLIPBOARD_SYNC"
        const val ACTION_STOP = "com.example.service.STOP_CLIPBOARD_SYNC"
        private const val CHANNEL_ID = "nexus_clipboard_channel"
        private const val NOTIFICATION_ID = 8806

        fun startService(context: Context) {
            val intent = Intent(context, ClipboardSyncService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ClipboardSyncService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val copiedText = clipData.getItemAt(0).text?.toString()
                if (!copiedText.isNullOrBlank()) {
                    Log.d("ClipboardSyncService", "Observed new clipboard copy: $copiedText")
                    syncClipboardToFirestore(copiedText)
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardSyncService", "Error reading clipboard item: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        firestoreRepository = FirestoreRepository(applicationContext)
        preferencesManager = PreferencesManager(applicationContext)

        createNotificationChannel()
        startForegroundWithType()
        registerClipboardListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                unregisterClipboardListener()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForegroundWithType()
            }
        }
        return START_STICKY
    }

    private fun registerClipboardListener() {
        try {
            clipboardManager.addPrimaryClipChangedListener(clipChangedListener)
            Log.d("ClipboardSyncService", "Clipboard listener registered successfully")
        } catch (e: Exception) {
            Log.e("ClipboardSyncService", "Failed to add clipboard listener: ${e.message}")
        }
    }

    private fun unregisterClipboardListener() {
        try {
            clipboardManager.removePrimaryClipChangedListener(clipChangedListener)
        } catch (e: Exception) {
            Log.e("ClipboardSyncService", "Failed to remove clipboard listener: ${e.message}")
        }
    }

    private fun syncClipboardToFirestore(text: String) {
        serviceScope.launch {
            try {
                val deviceId = preferencesManager.deviceIdFlow.firstOrNull() ?: "phone_b"
                val truncated = if (text.length > 100) "${text.take(100)}..." else text

                firestoreRepository.logAuditAction(
                    action = "CLIPBOARD_TEXT_SYNC",
                    initiatorRole = "MANAGED",
                    targetDeviceId = deviceId,
                    details = "Clipboard content copied: '$truncated'"
                )
            } catch (e: Exception) {
                Log.e("ClipboardSyncService", "Error syncing clipboard to Firestore: ${e.message}")
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
                "Nexus Clipboard Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Synchronizes copied text content for remote management paste"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS Clipboard Sync")
            .setContentText("Observing clipboard events for cross-device copy/paste")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterClipboardListener()
        super.onDestroy()
    }
}
