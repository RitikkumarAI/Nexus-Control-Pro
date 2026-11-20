package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PriorityWidgetStore
import com.example.data.WidgetPriorityDeviceSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android Home Screen App Widget displaying real-time connection status,
 * battery level, and last known location for up to 3 selected high-priority
 * managed devices.
 */
class NexusFleetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NexusFleetWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_PRIORITY_WIDGET"

        fun requestWidgetUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NexusFleetWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateAppWidget(context, manager, id)
                }
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val devices = PriorityWidgetStore.loadPriorityDevices(context).take(3)
            val views = RemoteViews(context.packageName, R.layout.widget_priority_devices)

            val syncedTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(PriorityWidgetStore.getLastSyncedTimestamp(context)))
            views.setTextViewText(
                R.id.widget_subtitle,
                "${devices.size} Priority • Synced $syncedTime"
            )

            // Open app when tapping header or container
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_container, launchPendingIntent)

            // Refresh button broadcast
            val refreshIntent = Intent(context, NexusFleetWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)

            if (devices.isEmpty()) {
                views.setViewVisibility(R.id.widget_empty_state, View.VISIBLE)
                views.setViewVisibility(R.id.widget_device_slot_1, View.GONE)
                views.setViewVisibility(R.id.widget_device_slot_2, View.GONE)
                views.setViewVisibility(R.id.widget_device_slot_3, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_empty_state, View.GONE)
                bindDeviceSlot(
                    views = views,
                    device = devices.getOrNull(0),
                    slotId = R.id.widget_device_slot_1,
                    dotId = R.id.slot1_status_dot,
                    nameId = R.id.slot1_name,
                    statusId = R.id.slot1_status,
                    batteryId = R.id.slot1_battery,
                    locationId = R.id.slot1_location,
                    launchPendingIntent = launchPendingIntent
                )
                bindDeviceSlot(
                    views = views,
                    device = devices.getOrNull(1),
                    slotId = R.id.widget_device_slot_2,
                    dotId = R.id.slot2_status_dot,
                    nameId = R.id.slot2_name,
                    statusId = R.id.slot2_status,
                    batteryId = R.id.slot2_battery,
                    locationId = R.id.slot2_location,
                    launchPendingIntent = launchPendingIntent
                )
                bindDeviceSlot(
                    views = views,
                    device = devices.getOrNull(2),
                    slotId = R.id.widget_device_slot_3,
                    dotId = R.id.slot3_status_dot,
                    nameId = R.id.slot3_name,
                    statusId = R.id.slot3_status,
                    batteryId = R.id.slot3_battery,
                    locationId = R.id.slot3_location,
                    launchPendingIntent = launchPendingIntent
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun bindDeviceSlot(
            views: RemoteViews,
            device: WidgetPriorityDeviceSnapshot?,
            slotId: Int,
            dotId: Int,
            nameId: Int,
            statusId: Int,
            batteryId: Int,
            locationId: Int,
            launchPendingIntent: PendingIntent
        ) {
            if (device == null) {
                views.setViewVisibility(slotId, View.GONE)
                return
            }
            views.setViewVisibility(slotId, View.VISIBLE)
            views.setOnClickPendingIntent(slotId, launchPendingIntent)

            views.setImageViewResource(
                dotId,
                if (device.isOnline) R.drawable.widget_status_online else R.drawable.widget_status_offline
            )

            val lockSuffix = if (device.isLocked) " • LOCKED" else ""
            val connText = if (device.isOnline) {
                "Online (${device.networkType})$lockSuffix"
            } else {
                "Offline$lockSuffix"
            }

            val chargingIcon = if (device.isCharging) " ⚡" else ""
            views.setTextViewText(nameId, device.deviceName)
            views.setTextViewText(statusId, connText)
            views.setTextViewText(batteryId, "${device.batteryLevel}%$chargingIcon")
            views.setTextViewText(locationId, "📍 ${device.lastLocation}")
        }
    }
}
