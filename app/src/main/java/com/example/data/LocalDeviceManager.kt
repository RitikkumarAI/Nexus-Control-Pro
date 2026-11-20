package com.example.data

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.model.NexusDevice
import java.io.File

class LocalDeviceManager(private val context: Context) {

    fun getLocalDeviceTelemetry(deviceId: String, deviceName: String, role: String): NexusDevice {
        val batteryInfo = getBatteryInfo()
        val storageInfo = getStorageInfo()
        val ramInfo = getRamInfo()
        val volumes = getAudioVolumes()
        val focusActive = isFocusModeActive()

        return NexusDevice(
            deviceId = deviceId,
            deviceName = deviceName,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            networkType = getNetworkType(),
            isOnline = isOnline(),
            totalStorageGb = storageInfo.first,
            usedStorageGb = storageInfo.second,
            totalRamGb = ramInfo.first,
            usedRamGb = ramInfo.second,
            uptimeHours = SystemClock.elapsedRealtime() / (1000 * 60 * 60),
            lastSeenTimestamp = System.currentTimeMillis(),
            role = role,
            focusModeActive = focusActive,
            ringerVolume = volumes.first,
            mediaVolume = volumes.second,
            alarmVolume = volumes.third
        )
    }

    private fun isFocusModeActive(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val filter = nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
                filter != NotificationManager.INTERRUPTION_FILTER_ALL
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun getAudioVolumes(): Triple<Int, Int, Int> {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return Triple(80, 70, 85)
            val ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING)
            val ringCur = am.getStreamVolume(AudioManager.STREAM_RING)
            val ringPct = if (ringMax > 0) (ringCur * 100) / ringMax else 80

            val mediaMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val mediaCur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val mediaPct = if (mediaMax > 0) (mediaCur * 100) / mediaMax else 70

            val alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val alarmCur = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val alarmPct = if (alarmMax > 0) (alarmCur * 100) / alarmMax else 85

            Triple(ringPct, mediaPct, alarmPct)
        } catch (e: Exception) {
            Triple(80, 70, 85)
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (scale > 0) (level * 100) / scale else 100
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(pct, isCharging)
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "Offline"
        val activeNetwork = cm.activeNetwork ?: return "Offline"
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (5G/4G)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getStorageInfo(): Pair<Double, Double> {
        return try {
            val path: File = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = String.format("%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            val usedGb = String.format("%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            Pair(totalGb, usedGb)
        } catch (e: Exception) {
            Pair(64.0, 24.5)
        }
    }

    private fun getRamInfo(): Pair<Double, Double> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalBytes = memInfo.totalMem
            val availBytes = memInfo.availMem
            val usedBytes = totalBytes - availBytes

            val totalGb = String.format("%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            val usedGb = String.format("%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0)).toDouble()
            Pair(totalGb, usedGb)
        } catch (e: Exception) {
            Pair(8.0, 3.8)
        }
    }
}
