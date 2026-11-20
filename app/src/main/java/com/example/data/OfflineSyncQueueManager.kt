package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.NexusDatabase
import com.example.data.local.SyncQueueEntity
import com.example.data.work.CommandSyncQueueWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages WorkManager-backed background synchronization for configuration updates,
 * geofence policies, and telemetry syncs. Buffers operations in Room whenever the
 * device is offline (or offline buffering test mode is active) and dispatches them
 * automatically via CommandSyncQueueWorker upon reconnection.
 */
class OfflineSyncQueueManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncQueueDao = NexusDatabase.getDatabase(context).syncQueueDao()

    val queueItems: StateFlow<List<SyncQueueEntity>> = syncQueueDao.observeQueue()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = syncQueueDao.observePendingCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isOfflineBufferMode = MutableStateFlow(false)
    val isOfflineBufferMode: StateFlow<Boolean> = _isOfflineBufferMode.asStateFlow()

    private val _isFlushing = MutableStateFlow(false)
    val isFlushing: StateFlow<Boolean> = _isFlushing.asStateFlow()

    private val _isDeviceOnline = MutableStateFlow(checkNetworkConnected())
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

    fun checkNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun setOfflineBufferMode(offline: Boolean) {
        _isOfflineBufferMode.value = offline
        _isDeviceOnline.value = !offline && checkNetworkConnected()
        if (!offline) {
            flushPendingQueue()
        }
    }

    /**
     * Enqueues a configuration/sync operation. If the device is online and not in
     * offline-buffer mode, executes immediately and records COMPLETED. Otherwise
     * buffers as PENDING in Room and schedules WorkManager with NetworkType.CONNECTED.
     */
    fun enqueueOrExecute(
        operationType: String,
        targetDeviceId: String,
        payloadSummary: String,
        payloadData: String = ""
    ) {
        scope.launch {
            val actuallyOnline = !_isOfflineBufferMode.value && checkNetworkConnected()
            _isDeviceOnline.value = actuallyOnline

            val itemId = "sync_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()

            if (!actuallyOnline) {
                val queuedItem = SyncQueueEntity(
                    id = itemId,
                    operationType = operationType,
                    targetDeviceId = targetDeviceId,
                    payloadSummary = payloadSummary,
                    payloadData = payloadData,
                    status = "PENDING",
                    createdAt = now
                )
                syncQueueDao.enqueue(queuedItem)
                firestoreRepository.logAuditAction(
                    action = "OFFLINE_QUEUED_$operationType",
                    initiatorRole = "ADMIN",
                    targetDeviceId = targetDeviceId,
                    details = "Device offline — buffered in WorkManager queue: $payloadSummary",
                    adminUser = "Ritik Kumar (Admin)",
                    outcome = "QUEUED",
                    success = true
                )
                CommandSyncQueueWorker.scheduleFlush(context)
            } else {
                val syncingItem = SyncQueueEntity(
                    id = itemId,
                    operationType = operationType,
                    targetDeviceId = targetDeviceId,
                    payloadSummary = payloadSummary,
                    payloadData = payloadData,
                    status = "SYNCING",
                    createdAt = now
                )
                syncQueueDao.enqueue(syncingItem)
                try {
                    firestoreRepository.syncPolicyConfiguration(
                        deviceId = targetDeviceId,
                        operationType = operationType,
                        payloadSummary = payloadSummary,
                        payloadData = payloadData
                    )
                    syncQueueDao.updateStatus(
                        id = itemId,
                        status = "COMPLETED",
                        executedAt = System.currentTimeMillis(),
                        retryCount = 0
                    )
                    firestoreRepository.logAuditAction(
                        action = "SYNC_$operationType",
                        initiatorRole = "ADMIN",
                        targetDeviceId = targetDeviceId,
                        details = "Synced immediately while online: $payloadSummary",
                        adminUser = "Ritik Kumar (Admin)",
                        outcome = "SUCCESS",
                        success = true
                    )
                } catch (e: Exception) {
                    syncQueueDao.updateStatus(
                        id = itemId,
                        status = "PENDING",
                        executedAt = null,
                        retryCount = 1
                    )
                    firestoreRepository.logAuditAction(
                        action = "OFFLINE_QUEUED_$operationType",
                        initiatorRole = "ADMIN",
                        targetDeviceId = targetDeviceId,
                        details = "Deferred to WorkManager retry queue: $payloadSummary",
                        adminUser = "Ritik Kumar (Admin)",
                        outcome = "QUEUED",
                        success = true
                    )
                    CommandSyncQueueWorker.scheduleFlush(context)
                }
            }
        }
    }

    /**
     * Reconnects and flushes all pending buffered operations via WorkManager and
     * immediate local execution for responsive dashboard feedback.
     */
    fun flushPendingQueue() {
        scope.launch {
            _isOfflineBufferMode.value = false
            _isDeviceOnline.value = checkNetworkConnected()
            _isFlushing.value = true
            try {
                val pending = syncQueueDao.getPendingItems()
                for (item in pending) {
                    syncQueueDao.updateStatus(item.id, "SYNCING", null, item.retryCount)
                    firestoreRepository.syncPolicyConfiguration(
                        deviceId = item.targetDeviceId,
                        operationType = item.operationType,
                        payloadSummary = item.payloadSummary,
                        payloadData = item.payloadData
                    )
                    syncQueueDao.updateStatus(
                        id = item.id,
                        status = "COMPLETED",
                        executedAt = System.currentTimeMillis(),
                        retryCount = item.retryCount
                    )
                    firestoreRepository.logAuditAction(
                        action = "SYNC_QUEUE_FLUSHED_${item.operationType}",
                        initiatorRole = "ADMIN",
                        targetDeviceId = item.targetDeviceId,
                        details = "Executed buffered WorkManager operation upon reconnection: ${item.payloadSummary}",
                        adminUser = "Ritik Kumar (Admin)",
                        outcome = "SUCCESS",
                        success = true
                    )
                }
                CommandSyncQueueWorker.scheduleFlush(context)
            } finally {
                _isFlushing.value = false
            }
        }
    }

    fun clearCompleted() {
        scope.launch {
            syncQueueDao.clearCompleted()
        }
    }
}
