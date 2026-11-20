package com.example.data

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.data.local.AppUsageEntity
import com.example.data.local.NexusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppUsageManager(private val context: Context) {

    private val db = NexusDatabase.getDatabase(context)
    private val appUsageDao = db.appUsageDao()
    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val scope = CoroutineScope(Dispatchers.IO)

    val allAppUsages: Flow<List<AppUsageEntity>> = appUsageDao.getAllAppUsages()

    /**
     * Queries UsageStatsManager for installed app usage duration in the last 24 hours,
     * resolves app names via PackageManager, and persists to local Room database.
     */
    fun refreshAppUsageStats() {
        scope.launch {
            try {
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (24 * 60 * 60 * 1000L) // Last 24 hours

                val usageStatsList: List<UsageStats> = usageStatsManager?.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                ) ?: emptyList()

                val appUsageEntities = mutableListOf<AppUsageEntity>()

                for (stats in usageStatsList) {
                    val packageName = stats.packageName
                    if (stats.totalTimeInForeground <= 0) continue

                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        appUsageEntities.add(
                            AppUsageEntity(
                                packageName = packageName,
                                appName = appName,
                                usageTimeMs = stats.totalTimeInForeground,
                                lastTimeUsed = stats.lastTimeUsed,
                                isSystemApp = isSystem
                            )
                        )
                    } catch (e: PackageManager.NameNotFoundException) {
                        // Package uninstalled or hidden system component
                        continue
                    }
                }

                if (appUsageEntities.isNotEmpty()) {
                    appUsageDao.insertAppUsages(appUsageEntities)
                    Log.d("AppUsageManager", "Persisted ${appUsageEntities.size} app usage entries to Room DB")
                }
            } catch (e: Exception) {
                Log.e("AppUsageManager", "Error querying UsageStatsManager: ${e.message}")
            }
        }
    }
}
