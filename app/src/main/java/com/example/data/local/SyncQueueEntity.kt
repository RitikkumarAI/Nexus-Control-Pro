package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    val operationType: String, // SETTINGS_SYNC, GEOFENCE_UPDATE, FOCUS_MODE_UPDATE, AUDIO_POLICY_UPDATE, TELEMETRY_REFRESH
    val targetDeviceId: String,
    val payloadSummary: String,
    val payloadData: String = "",
    val status: String = "PENDING", // PENDING, SYNCING, COMPLETED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val executedAt: Long? = null
)
