package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SessionRepository(context: Context) {

    private val sessionDao: SessionDao =
        NexusDatabase.getDatabase(context).sessionDao()

    val allSessions: Flow<List<SessionHistoryEntity>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: SessionHistoryEntity) {
        sessionDao.insertSession(session)
    }

    suspend fun clearHistory() {
        sessionDao.clearAll()
    }
}
