package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "session_history")
data class SessionHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L,
    val interactionType: String = "SCREEN_SHARE", // "SCREEN_SHARE", "FILE_MANAGER", "MIC_LISTEN", etc.
    val targetDeviceId: String = "phone_b",
    val adminDeviceId: String = "phone_a",
    val details: String = "Remote session active"
)
