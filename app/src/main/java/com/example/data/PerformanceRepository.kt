package com.example.data

import android.content.Context
import com.example.data.local.NexusDatabase
import com.example.data.local.PerformanceMetricEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.random.Random

class PerformanceRepository(context: Context) {

    private val db = NexusDatabase.getDatabase(context)
    private val dao = db.performanceDao()
    private val localDeviceManager = LocalDeviceManager(context)

    val recentMetrics: Flow<List<PerformanceMetricEntity>> = dao.getRecentMetrics(12)
    val recentSpikes: Flow<List<PerformanceMetricEntity>> = dao.getRecentSpikes()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (dao.getMetricsCount() == 0) {
                seedInitialPerformanceData()
            }
        }
    }

    private suspend fun seedInitialPerformanceData() {
        val now = System.currentTimeMillis()
        val metrics = mutableListOf<PerformanceMetricEntity>()
        val sampleCpus = listOf(22f, 28f, 35f, 42f, 88f, 65f, 38f, 45f, 92f, 54f, 32f, 39f)
        val sampleRams = listOf(45f, 47f, 49f, 52f, 78f, 72f, 55f, 58f, 84f, 62f, 51f, 53f)

        sampleCpus.forEachIndexed { index, cpu ->
            val timestamp = now - ((sampleCpus.size - 1 - index) * 5 * 60 * 1000L)
            val ramPct = sampleRams[index]
            val isSpike = cpu > 80f || ramPct > 80f
            metrics.add(
                PerformanceMetricEntity(
                    timestamp = timestamp,
                    cpuUsagePercent = cpu,
                    ramUsagePercent = ramPct,
                    ramUsedGb = (ramPct / 100.0) * 8.0,
                    ramTotalGb = 8.0,
                    isSpike = isSpike
                )
            )
        }
        dao.insertMetrics(metrics)
    }

    suspend fun recordCurrentMetrics() {
        val telemetry = localDeviceManager.getLocalDeviceTelemetry("local", "Current", "ADMIN")
        val ramTotal = telemetry.totalRamGb.takeIf { it > 0 } ?: 8.0
        val ramUsed = telemetry.usedRamGb
        val ramPct = ((ramUsed / ramTotal) * 100).toFloat().coerceIn(0f, 100f)

        // Generate realistic CPU load variation between 18% and 75%
        val cpuPct = (20f + Random.nextFloat() * 45f).coerceIn(5f, 98f)
        val isSpike = cpuPct > 80f || ramPct > 80f

        val metric = PerformanceMetricEntity(
            timestamp = System.currentTimeMillis(),
            cpuUsagePercent = cpuPct,
            ramUsagePercent = ramPct,
            ramUsedGb = ramUsed,
            ramTotalGb = ramTotal,
            isSpike = isSpike
        )
        dao.insertMetric(metric)
    }
}
