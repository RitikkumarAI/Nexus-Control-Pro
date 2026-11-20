package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: PerformanceMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<PerformanceMetricEntity>)

    @Query("SELECT * FROM performance_metrics ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMetrics(limit: Int = 12): Flow<List<PerformanceMetricEntity>>

    @Query("SELECT * FROM performance_metrics WHERE isSpike = 1 ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSpikes(): Flow<List<PerformanceMetricEntity>>

    @Query("SELECT COUNT(*) FROM performance_metrics")
    suspend fun getMetricsCount(): Int

    @Query("DELETE FROM performance_metrics WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeOldMetrics(cutoffTimestamp: Long)
}
