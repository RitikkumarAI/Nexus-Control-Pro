package com.example.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsRepository(private val context: Context) {

    suspend fun getInstalledApplications(): List<AppItem> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        val usageStatsMap = getUsageStatsMap()

        val appList = mutableListOf<AppItem>()
        val processedPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            if (processedPackages.contains(packageName)) continue
            processedPackages.add(packageName)

            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val appInfo = packageInfo.applicationInfo ?: continue
                val label = packageManager.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val usageTimeMs = usageStatsMap[packageName] ?: 0L

                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                val sensitivePerms = getSensitivePermissionsForPackage(packageName)

                appList.add(
                    AppItem(
                        packageName = packageName,
                        label = label,
                        versionName = packageInfo.versionName ?: "1.0",
                        versionCode = versionCode,
                        isSystemApp = isSystem,
                        installTime = packageInfo.firstInstallTime,
                        usageTimeMillisMs = usageTimeMs,
                        sensitivePermissions = sensitivePerms
                    )
                )
            } catch (e: Exception) {
                // Ignore unresolvable packages
            }
        }

        appList.sortedBy { it.label.lowercase() }
    }

    fun getSensitivePermissionsForPackage(packageName: String): List<com.example.model.SensitivePermissionState> {
        val pm = context.packageManager
        val targets = listOf(
            Triple("android.permission.ACCESS_FINE_LOCATION", "Location", "LOCATION"),
            Triple("android.permission.CAMERA", "Camera", "CAMERA"),
            Triple("android.permission.RECORD_AUDIO", "Microphone", "MICROPHONE"),
            Triple("android.permission.READ_CONTACTS", "Contacts", "CONTACTS")
        )
        return targets.map { (perm, name, icon) ->
            val granted = pm.checkPermission(perm, packageName) == PackageManager.PERMISSION_GRANTED
            com.example.model.SensitivePermissionState(
                permissionKey = perm,
                displayName = name,
                isGranted = granted,
                iconType = icon
            )
        }
    }

    private fun getUsageStatsMap(): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000) // last 24 hours

        val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val resultMap = mutableMapOf<String, Long>()
        if (stats != null) {
            for (usageStat in stats) {
                val existing = resultMap[usageStat.packageName] ?: 0L
                resultMap[usageStat.packageName] = existing + usageStat.totalTimeInForeground
            }
        }
        return resultMap
    }

    fun hasUsageAccessPermission(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        return !stats.isNullOrEmpty()
    }
}
