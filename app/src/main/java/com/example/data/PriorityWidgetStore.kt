package com.example.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.example.model.NexusDevice
import com.example.widget.NexusFleetWidgetProvider
import org.json.JSONArray
import org.json.JSONObject

data class WidgetPriorityDeviceSnapshot(
    val deviceId: String,
    val deviceName: String,
    val isOnline: Boolean,
    val isLocked: Boolean,
    val networkType: String,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val lastLocation: String
)

/**
 * Persists selected high-priority managed devices for instant, reliable rendering
 * by the Android Home Screen App Widget (NexusFleetWidgetProvider).
 */
object PriorityWidgetStore {

    private const val PREFS_NAME = "nexus_priority_widget_prefs"
    private const val KEY_PRIORITY_DEVICES_JSON = "priority_devices_json"
    private const val KEY_LAST_SYNCED_MS = "last_synced_ms"

    fun savePriorityDevices(context: Context, devices: List<NexusDevice>) {
        val prioritized = devices.filter { it.isHighPriority }.take(3)
        val array = JSONArray()
        for (device in prioritized) {
            val obj = JSONObject().apply {
                put("deviceId", device.deviceId)
                put("deviceName", device.deviceName)
                put("isOnline", device.isOnline)
                put("isLocked", device.isLocked)
                put("networkType", device.networkType)
                put("batteryLevel", device.batteryLevel)
                put("isCharging", device.isCharging)
                put(
                    "lastLocation",
                    device.lastLocationAddress?.takeIf { it.isNotBlank() } ?: "Location unavailable"
                )
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PRIORITY_DEVICES_JSON, array.toString())
            .putLong(KEY_LAST_SYNCED_MS, System.currentTimeMillis())
            .apply()

        NexusFleetWidgetProvider.requestWidgetUpdate(context)
    }

    fun loadPriorityDevices(context: Context): List<WidgetPriorityDeviceSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PRIORITY_DEVICES_JSON, null)
        if (raw.isNullOrBlank()) {
            return defaultSnapshots()
        }
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<WidgetPriorityDeviceSnapshot>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    WidgetPriorityDeviceSnapshot(
                        deviceId = obj.optString("deviceId", "dev_$i"),
                        deviceName = obj.optString("deviceName", "Managed Device"),
                        isOnline = obj.optBoolean("isOnline", true),
                        isLocked = obj.optBoolean("isLocked", false),
                        networkType = obj.optString("networkType", "4G"),
                        batteryLevel = obj.optInt("batteryLevel", 80),
                        isCharging = obj.optBoolean("isCharging", false),
                        lastLocation = obj.optString("lastLocation", "Unknown location")
                    )
                )
            }
            result
        } catch (_: Exception) {
            defaultSnapshots()
        }
    }

    fun getLastSyncedTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNCED_MS, System.currentTimeMillis())
    }

    /**
     * Prompts the Android launcher to pin the Nexus Priority Fleet Widget directly to the
     * user's home screen when supported (Android 8.0+ / API 26+).
     */
    fun requestPinWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, NexusFleetWidgetProvider::class.java)
        return appWidgetManager.requestPinAppWidget(provider, null, null)
    }

    private fun defaultSnapshots(): List<WidgetPriorityDeviceSnapshot> = listOf(
        WidgetPriorityDeviceSnapshot(
            deviceId = "phone_a",
            deviceName = "Field Phone A",
            isOnline = true,
            isLocked = false,
            networkType = "5G",
            batteryLevel = 87,
            isCharging = true,
            lastLocation = "Koramangala Tech Park, Bengaluru"
        ),
        WidgetPriorityDeviceSnapshot(
            deviceId = "tablet_ops_02",
            deviceName = "Warehouse Tablet B2",
            isOnline = true,
            isLocked = false,
            networkType = "Wi-Fi",
            batteryLevel = 64,
            isCharging = false,
            lastLocation = "Whitefield Logistics Hub, Sector 4"
        ),
        WidgetPriorityDeviceSnapshot(
            deviceId = "dispatch_unit_03",
            deviceName = "Dispatch Pixel C3",
            isOnline = false,
            isLocked = true,
            networkType = "Offline",
            batteryLevel = 23,
            isCharging = false,
            lastLocation = "MG Road Depot • 14m ago"
        )
    )
}
