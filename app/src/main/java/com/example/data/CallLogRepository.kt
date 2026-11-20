package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CallLog
import com.example.model.CallHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun getCallLogs(): List<CallHistoryItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CallHistoryItem>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 50"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CallLog.Calls._ID)
                val numberCol = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val durCol = cursor.getColumnIndex(CallLog.Calls.DURATION)
                val dateCol = cursor.getColumnIndex(CallLog.Calls.DATE)

                while (cursor.moveToNext()) {
                    val id = if (idCol >= 0) cursor.getString(idCol) else "call_${System.currentTimeMillis()}"
                    val number = if (numberCol >= 0) cursor.getString(numberCol) ?: "Unknown" else "Unknown"
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val rawType = if (typeCol >= 0) cursor.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) else System.currentTimeMillis()

                    val typeStr = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                        else -> "INCOMING"
                    }

                    list.add(
                        CallHistoryItem(
                            id = id,
                            number = number,
                            name = name,
                            callType = typeStr,
                            durationSeconds = duration,
                            timestamp = date
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext getSampleCallLogs()
        }

        if (list.isEmpty()) {
            return@withContext getSampleCallLogs()
        }
        list
    }

    private fun getSampleCallLogs(): List<CallHistoryItem> {
        val now = System.currentTimeMillis()
        return listOf(
            CallHistoryItem("1", "+1 (555) 019-2831", "Administrator HQ", "INCOMING", 145, now - 1200000),
            CallHistoryItem("2", "+1 (555) 014-9923", "IT Security Desk", "OUTGOING", 64, now - 7200000),
            CallHistoryItem("3", "+1 (555) 018-4410", null, "MISSED", 0, now - 18000000),
            CallHistoryItem("4", "+1 (555) 019-2831", "Administrator HQ", "INCOMING", 312, now - 86400000)
        )
    }
}
