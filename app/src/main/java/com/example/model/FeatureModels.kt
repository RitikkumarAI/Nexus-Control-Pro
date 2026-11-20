package com.example.model

data class AppItem(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installTime: Long,
    val usageTimeMillisMs: Long = 0,
    val iconUri: String? = null,
    val sensitivePermissions: List<SensitivePermissionState> = emptyList()
)

data class FileNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String? = null
)

data class MediaItemData(
    val id: Long,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val contentUri: String,
    val isVideo: Boolean
)

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long,
    val provider: String = "GPS",
    val addressLabel: String = "Lat: $latitude, Lng: $longitude"
)

data class CallHistoryItem(
    val id: String,
    val number: String,
    val name: String?,
    val callType: String, // INCOMING, OUTGOING, MISSED
    val durationSeconds: Long,
    val timestamp: Long
)
