package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.GeofencePushAlert

class GeofenceNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "nexus_geofence_alerts"
        const val CHANNEL_NAME = "Geofence Boundary Alerts"
        const val CHANNEL_DESC = "High-priority alerts when a managed device moves outside its configured geofence boundary."
    }

    init {
        ensureNotificationChannel()
    }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Dispatches a high-priority local push notification on the admin device
     * when a managed device crosses outside its established geofence boundary.
     */
    fun dispatchGeofenceBreachNotification(alert: GeofencePushAlert): Boolean {
        if (!hasNotificationPermission()) {
            return false
        }

        ensureNotificationChannel()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("geofence_alert_id", alert.alertId)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.alertId.hashCode(),
            launchIntent,
            pendingIntentFlags
        )

        val title = "Geofence Boundary Exit: ${alert.deviceName}"
        val body = alert.message.ifBlank {
            "${alert.deviceName} exited ${alert.zoneName} (${alert.distanceFromCenterMeters.toInt()}m outside ${alert.boundaryRadiusMeters.toInt()}m boundary)."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(
                alert.alertId.hashCode().and(0x7FFFFFFF),
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
