package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_metrics")
data class PerformanceMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cpuUsagePercent: Float,
    val ramUsagePercent: Float,
    val ramUsedGb: Double,
    val ramTotalGb: Double,
    val isSpike: Boolean = false
)
