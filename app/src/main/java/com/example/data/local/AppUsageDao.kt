package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsages(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage ORDER BY usageTimeMs DESC")
    fun getAllAppUsages(): Flow<List<AppUsageEntity>>

    @Query("DELETE FROM app_usage")
    suspend fun clearAll()
}
