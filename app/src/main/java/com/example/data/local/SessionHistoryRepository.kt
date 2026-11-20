package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SessionHistoryRepository(context: Context) {

    private val sessionHistoryDao: SessionHistoryDao =
        NexusDatabase.getDatabase(context).sessionHistoryDao()

    val allSessions: Flow<List<SessionHistoryEntity>> = sessionHistoryDao.getAllSessions()

    suspend fun logSession(
        interactionType: String,
        durationSeconds: Long,
        targetDeviceId: String = "phone_b",
        adminDeviceId: String = "phone_a",
        details: String = "Remote assistance session completed"
    ) {
        val entity = SessionHistoryEntity(
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            interactionType = interactionType,
            targetDeviceId = targetDeviceId,
            adminDeviceId = adminDeviceId,
            details = details
        )
        sessionHistoryDao.insertSession(entity)
    }

    suspend fun clearHistory() {
        sessionHistoryDao.clearAll()
    }
}
