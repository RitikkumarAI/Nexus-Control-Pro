package com.example.data.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.FirestoreRepository
import com.example.data.LocalDeviceManager
import com.example.data.local.NexusDatabase
import com.example.data.local.SyncQueueEntity
import java.util.concurrent.TimeUnit

/**
 * WorkManager-based background sync worker that flushes buffered configuration
 * and telemetry synchronization operations once network connectivity is restored.
 */
class CommandSyncQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = NexusDatabase.getDatabase(applicationContext)
        val syncQueueDao = db.syncQueueDao()
        val firestoreRepository = FirestoreRepository(applicationContext)
        val localDeviceManager = LocalDeviceManager(applicationContext)

        val pendingItems = syncQueueDao.getPendingItems()
        if (pendingItems.isEmpty()) {
            Log.d(TAG, "No pending queued sync items to process.")
            return Result.success()
        }

        var hadTransientFailure = false

        for (item in pendingItems) {
            try {
                syncQueueDao.updateStatus(
                    id = item.id,
                    status = "SYNCING",
                    executedAt = null,
                    retryCount = item.retryCount
                )

                dispatchQueuedOperation(item, firestoreRepository, localDeviceManager)

                val executedAt = System.currentTimeMillis()
                syncQueueDao.updateStatus(
                    id = item.id,
                    status = "COMPLETED",
                    executedAt = executedAt,
                    retryCount = item.retryCount
                )

                firestoreRepository.logAuditAction(
                    action = "SYNC_QUEUE_EXECUTED_${item.operationType}",
                    initiatorRole = "ADMIN",
                    targetDeviceId = item.targetDeviceId,
                    details = "WorkManager synced buffered operation: ${item.payloadSummary}",
                    adminUser = "Ritik Kumar (Admin)",
                    outcome = "SUCCESS",
                    success = true
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync queued item ${item.id}: ${e.message}")
                val nextRetry = item.retryCount + 1
                val terminalStatus = if (nextRetry >= MAX_RETRIES) "FAILED" else "PENDING"
                syncQueueDao.updateStatus(
                    id = item.id,
                    status = terminalStatus,
                    executedAt = if (terminalStatus == "FAILED") System.currentTimeMillis() else null,
                    retryCount = nextRetry
                )
                if (terminalStatus == "FAILED") {
                    firestoreRepository.logAuditAction(
                        action = "SYNC_QUEUE_FAILED_${item.operationType}",
                        initiatorRole = "ADMIN",
                        targetDeviceId = item.targetDeviceId,
                        details = "Buffered operation failed after $nextRetry attempts: ${item.payloadSummary}",
                        adminUser = "Ritik Kumar (Admin)",
                        outcome = "FAILED",
                        success = false
                    )
                } else {
                    hadTransientFailure = true
                }
            }
        }

        return if (hadTransientFailure) Result.retry() else Result.success()
    }

    private suspend fun dispatchQueuedOperation(
        item: SyncQueueEntity,
        firestoreRepository: FirestoreRepository,
        localDeviceManager: LocalDeviceManager
    ) {
        when (item.operationType) {
            "FOCUS_MODE_UPDATE" -> {
                val enable = item.payloadData.equals("true", ignoreCase = true)
                firestoreRepository.sendFocusModeCommand(item.targetDeviceId, enable)
            }
            "AUDIO_POLICY_UPDATE" -> {
                val parts = item.payloadData.split(",").mapNotNull { it.trim().toIntOrNull() }
                val ringer = parts.getOrNull(0)
                val media = parts.getOrNull(1)
                val alarm = parts.getOrNull(2)
                firestoreRepository.sendVolumeCommand(item.targetDeviceId, ringer, media, alarm)
            }
            "TELEMETRY_REFRESH" -> {
                val telemetry = localDeviceManager.getLocalDeviceTelemetry(
                    deviceId = item.targetDeviceId,
                    deviceName = "Field Phone A",
                    role = "MANAGED"
                )
                firestoreRepository.updateDeviceTelemetry(telemetry)
            }
            "SETTINGS_SYNC", "GEOFENCE_UPDATE" -> {
                firestoreRepository.syncPolicyConfiguration(
                    deviceId = item.targetDeviceId,
                    operationType = item.operationType,
                    payloadSummary = item.payloadSummary,
                    payloadData = item.payloadData
                )
            }
            else -> {
                firestoreRepository.syncPolicyConfiguration(
                    deviceId = item.targetDeviceId,
                    operationType = item.operationType,
                    payloadSummary = item.payloadSummary,
                    payloadData = item.payloadData
                )
            }
        }
    }

    companion object {
        private const val TAG = "CommandSyncQueueWorker"
        private const val WORK_NAME = "nexus_offline_command_sync_queue"
        private const val MAX_RETRIES = 3

        fun scheduleFlush(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<CommandSyncQueueWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
            Log.d(TAG, "Scheduled WorkManager sync flush ($WORK_NAME) with NetworkType.CONNECTED constraint.")
        }
    }
}
