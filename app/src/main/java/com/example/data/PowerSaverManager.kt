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

data class PowerSaverState(
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveModeActive: Boolean = false,
    val syncIntervalMs: Long = 15000L
)

class PowerSaverManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _powerSaverState = MutableStateFlow(PowerSaverState())
    val powerSaverState: StateFlow<PowerSaverState> = _powerSaverState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                evaluatePowerSaverSettings(intent)
            }
        }
    }

    fun startMonitoring() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = context.registerReceiver(batteryReceiver, filter)
            if (stickyIntent != null) {
                evaluatePowerSaverSettings(stickyIntent)
            }
        } catch (e: Exception) {
            Log.e("PowerSaverManager", "Failed to register battery observer: ${e.message}")
        }
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Log.e("PowerSaverManager", "Failed to unregister battery observer: ${e.message}")
        }
    }

    fun getCalculatedSyncIntervalMs(): Long {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val pct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return calculateInterval(pct, isCharging)
    }

    private fun evaluatePowerSaverSettings(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val pct = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val intervalMs = calculateInterval(pct, isCharging)
        val isSaverActive = !isCharging && pct < 20

        val state = PowerSaverState(
            batteryPercentage = pct,
            isCharging = isCharging,
            isPowerSaveModeActive = isSaverActive,
            syncIntervalMs = intervalMs
        )

        _powerSaverState.value = state
        Log.d("PowerSaverManager", "PowerSaver updated: interval=${intervalMs}ms, saverActive=$isSaverActive")
    }

    private fun calculateInterval(batteryPct: Int, isCharging: Boolean): Long {
        if (isCharging) return 15000L // Fast 15s sync when charging

        return when {
            batteryPct > 50 -> 15000L // 15 seconds for normal operation
            batteryPct in 20..50 -> 30000L // 30 seconds for moderate battery
            else -> 60000L // 60 seconds ultra power saver for battery < 20%
        }
    }
}
