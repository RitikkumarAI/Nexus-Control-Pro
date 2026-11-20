package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity)

    @Query("SELECT * FROM offline_sync_queue ORDER BY createdAt DESC LIMIT 25")
    fun observeQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM offline_sync_queue WHERE status IN ('PENDING', 'SYNCING')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM offline_sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Query("UPDATE offline_sync_queue SET status = :status, executedAt = :executedAt, retryCount = :retryCount WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, executedAt: Long?, retryCount: Int)

    @Query("DELETE FROM offline_sync_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
