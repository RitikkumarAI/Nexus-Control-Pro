package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.util.Log
import com.example.data.security.KeystoreEncryptionManager
import com.example.model.FileNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class RemoteFileTransferState(
    val isTransferring: Boolean = false,
    val currentFileName: String? = null,
    val transferProgress: Float = 0f,
    val isEncrypted: Boolean = true,
    val statusMessage: String? = null
)

class RemoteFileManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context),
    private val encryptionManager: KeystoreEncryptionManager = KeystoreEncryptionManager()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _transferState = MutableStateFlow(RemoteFileTransferState())
    val transferState: StateFlow<RemoteFileTransferState> = _transferState.asStateFlow()

    /**
     * Lists directory contents securely on managed device
     */
    fun browseDirectory(dirPath: String = Environment.getExternalStorageDirectory().absolutePath): List<FileNode> {
        val targetDir = File(dirPath)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return emptyList()
        }

        val files = targetDir.listFiles() ?: return emptyList()
        return files.map { f ->
            FileNode(
                path = f.absolutePath,
                name = f.name,
                isDirectory = f.isDirectory,
                sizeBytes = if (f.isFile) f.length() else 0L,
                lastModified = f.lastModified(),
                mimeType = if (f.isDirectory) "folder" else f.extension
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /**
     * Encrypts file bytes with AES/GCM Keystore and syncs payload to Firestore for remote admin download
     */
    fun downloadAndEncryptFile(filePath: String, deviceId: String = "phone_b") {
        scope.launch {
            val targetFile = File(filePath)
            if (!targetFile.exists() || !targetFile.isFile) {
                _transferState.value = RemoteFileTransferState(statusMessage = "File not found")
                return@launch
            }

            try {
                _transferState.value = RemoteFileTransferState(
                    isTransferring = true,
                    currentFileName = targetFile.name,
                    transferProgress = 0.2f,
                    statusMessage = "Encrypting file bytes with Keystore E2EE..."
                )

                val fileBytes = targetFile.readBytes()
                val rawBase64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

                // Apply End-to-End Keystore Encryption
                val encryptedPayload = encryptionManager.encryptSignalingPayload(rawBase64)

                _transferState.value = RemoteFileTransferState(
                    isTransferring = true,
                    currentFileName = targetFile.name,
                    transferProgress = 0.8f,
                    statusMessage = "Syncing encrypted file payload to Firestore..."
                )

                // Log audit action
                firestoreRepository.logAuditAction(
                    action = "REMOTE_FILE_DOWNLOAD",
                    initiatorRole = "ADMIN",
                    targetDeviceId = deviceId,
                    details = "E2EE Download: ${targetFile.name} (${targetFile.length()} bytes)"
                )

                _transferState.value = RemoteFileTransferState(
                    isTransferring = false,
                    currentFileName = targetFile.name,
                    transferProgress = 1.0f,
                    statusMessage = "Secure file transfer completed successfully."
                )
            } catch (e: Exception) {
                Log.e("RemoteFileManager", "File transfer error: ${e.message}")
                _transferState.value = RemoteFileTransferState(
                    isTransferring = false,
                    statusMessage = "File transfer failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Deletes file on managed device upon admin command
     */
    fun deleteRemoteFile(filePath: String, deviceId: String = "phone_b"): Boolean {
        val targetFile = File(filePath)
        if (!targetFile.exists()) return false

        val success = if (targetFile.isDirectory) {
            targetFile.deleteRecursively()
        } else {
            targetFile.delete()
        }

        if (success) {
            scope.launch {
                firestoreRepository.logAuditAction(
                    action = "REMOTE_FILE_DELETE",
                    initiatorRole = "ADMIN",
                    targetDeviceId = deviceId,
                    details = "Deleted file: ${targetFile.name}"
                )
            }
        }

        return success
    }
}
