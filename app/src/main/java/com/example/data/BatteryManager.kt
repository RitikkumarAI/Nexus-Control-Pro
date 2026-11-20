package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager as AndroidBatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceBatteryState(
    val levelPercentage: Int = 100,
    val isCharging: Boolean = false,
    val statusText: String = "Normal"
)

class BatteryManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _batteryState = MutableStateFlow(DeviceBatteryState())
    val batteryState: StateFlow<DeviceBatteryState> = _batteryState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                handleBatteryChange(intent)
            }
        }
    }

    fun registerBatteryReceiver(deviceId: String) {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = context.registerReceiver(batteryReceiver, filter)
            if (stickyIntent != null) {
                handleBatteryChange(stickyIntent, deviceId)
            }
        } catch (e: Exception) {
            Log.e("BatteryManager", "Failed to register receiver: ${e.message}")
        }
    }

    fun unregisterBatteryReceiver() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.e("BatteryManager", "Failed to unregister receiver: ${e.message}")
        }
    }

    private fun handleBatteryChange(intent: Intent, deviceId: String = "phone_b") {
        val level = intent.getIntExtra(AndroidBatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(AndroidBatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(AndroidBatteryManager.EXTRA_STATUS, -1)

        val percentage = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            100
        }

        val isCharging = status == AndroidBatteryManager.BATTERY_STATUS_CHARGING ||
                status == AndroidBatteryManager.BATTERY_STATUS_FULL

        val statusText = if (isCharging) "Charging" else "Discharging"

        val updated = DeviceBatteryState(
            levelPercentage = percentage,
            isCharging = isCharging,
            statusText = statusText
        )

        _batteryState.value = updated

        // Push battery percentage and charging state updates to real-time Firestore device dashboard
        scope.launch {
            try {
                firestoreRepository.updateDeviceBatteryState(
                    deviceId = deviceId,
                    batteryLevel = percentage,
                    isCharging = isCharging
                )
            } catch (e: Exception) {
                Log.e("BatteryManager", "Firestore battery update error: ${e.message}")
            }
        }
    }
}
