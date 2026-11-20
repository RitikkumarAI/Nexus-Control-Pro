package com.example.data.work

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.FirestoreRepository
import com.example.data.LocalDeviceManager
import com.example.data.PreferencesManager
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class TelemetrySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestoreRepository = FirestoreRepository(context)
    private val localDeviceManager = LocalDeviceManager(context)
    private val prefsManager = PreferencesManager(context)

    override suspend fun doWork(): Result {
        return try {
            val batteryLevel = getBatteryLevel()
            val role = prefsManager.roleFlow.firstOrNull()?.name ?: "MANAGED"
            val deviceId = prefsManager.deviceIdFlow.firstOrNull() ?: "phone_b"
            val deviceName = prefsManager.deviceNameFlow.firstOrNull() ?: "Managed Device"

            Log.d("TelemetrySyncWorker", "Executing periodic telemetry sync. Battery: $batteryLevel%")

            val telemetry = localDeviceManager.getLocalDeviceTelemetry(deviceId, deviceName, role)
            firestoreRepository.updateDeviceTelemetry(telemetry)

            // Dynamically scale periodic schedule based on remaining battery level
            scheduleDynamicSync(applicationContext, batteryLevel)

            Result.success()
        } catch (e: Exception) {
            Log.e("TelemetrySyncWorker", "Error executing WorkManager sync: ${e.message}")
            Result.retry()
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            100
        }
    }

    companion object {
        const val WORK_NAME = "nexus_periodic_telemetry_sync"

        fun scheduleDynamicSync(context: Context, batteryPercentage: Int) {
            val intervalMinutes = when {
                batteryPercentage > 50 -> 15L // 15 minutes for normal high-battery state
                batteryPercentage in 20..50 -> 30L // 30 minutes for medium battery
                else -> 60L // 60 minutes for battery saver state (<20%)
            }

            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            if (batteryPercentage < 20) {
                constraintsBuilder.setRequiresBatteryNotLow(true)
            }

            val syncRequest = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(
                intervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraintsBuilder.build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            Log.d("TelemetrySyncWorker", "Scheduled WorkManager sync every ${intervalMinutes}m (Battery: $batteryPercentage%)")
        }
    }
}
