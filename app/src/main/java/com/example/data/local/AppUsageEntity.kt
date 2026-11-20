package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val lastTimeUsed: Long,
    val isSystemApp: Boolean
)
