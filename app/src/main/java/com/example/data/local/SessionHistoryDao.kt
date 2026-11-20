package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionHistoryEntity)

    @Query("SELECT * FROM session_history ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionHistoryEntity>>

    @Query("DELETE FROM session_history")
    suspend fun clearAll()
}
