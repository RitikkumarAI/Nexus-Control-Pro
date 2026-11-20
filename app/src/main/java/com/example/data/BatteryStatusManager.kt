package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BatteryInfoState(
    val levelPercentage: Int = 100,
    val isCharging: Boolean = false,
    val plugType: String = "Battery"
)

class BatteryStatusManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val deviceId: String
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _batteryState = MutableStateFlow(BatteryInfoState())
    val batteryState: StateFlow<BatteryInfoState> = _batteryState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                parseBatteryIntent(intent)
            }
        }
    }

    fun startListening() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = context.registerReceiver(batteryReceiver, filter)
            if (stickyIntent != null) {
                parseBatteryIntent(stickyIntent)
            }
        } catch (e: Exception) {
            Log.e("BatteryStatusManager", "Failed to register battery receiver: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.e("BatteryStatusManager", "Failed to unregister battery receiver: ${e.message}")
        }
    }

    private fun parseBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        val percentage = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugTypeStr = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Discharging"
        }

        val info = BatteryInfoState(
            levelPercentage = percentage,
            isCharging = isCharging,
            plugType = plugTypeStr
        )

        _batteryState.value = info
        syncToFirestore(info)
    }

    private fun syncToFirestore(info: BatteryInfoState) {
        scope.launch {
            try {
                firestoreRepository.updateDeviceBatteryState(
                    deviceId = deviceId,
                    batteryLevel = info.levelPercentage,
                    isCharging = info.isCharging
                )
            } catch (e: Exception) {
                Log.e("BatteryStatusManager", "Firestore battery sync failed: ${e.message}")
            }
        }
    }
}
