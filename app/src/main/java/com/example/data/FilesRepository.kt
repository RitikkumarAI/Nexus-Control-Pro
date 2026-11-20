package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.model.FileNode
import com.example.model.MediaItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FilesRepository(private val context: Context) {

    suspend fun listDirectory(dirPath: String? = null): List<FileNode> = withContext(Dispatchers.IO) {
        val targetDir = if (dirPath.isNullOrBlank()) {
            Environment.getExternalStorageDirectory()
        } else {
            File(dirPath)
        }

        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext getSampleFiles()
        }

        val files = targetDir.listFiles() ?: return@withContext emptyList()
        val nodes = mutableListOf<FileNode>()

        for (f in files) {
            if (f.name.startsWith(".")) continue
            nodes.add(
                FileNode(
                    path = f.absolutePath,
                    name = f.name,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isDirectory) 0L else f.length(),
                    lastModified = f.lastModified(),
                    mimeType = if (f.isDirectory) "folder" else getMimeType(f.name)
                )
            )
        }

        nodes.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun getMediaItems(): List<MediaItemData> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItemData>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC LIMIT 100"

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Media_$id"
                    val mime = cursor.getString(mimeColumn) ?: "image/jpeg"
                    val size = cursor.getLong(sizeColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000
                    val mediaType = cursor.getInt(typeColumn)
                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

                    val contentUri: Uri = if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    mediaList.add(
                        MediaItemData(
                            id = id,
                            displayName = name,
                            mimeType = mime,
                            sizeBytes = size,
                            dateAddedMs = dateAdded,
                            contentUri = contentUri.toString(),
                            isVideo = isVideo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback sample data if MediaStore restricted
            return@withContext getSampleMediaItems()
        }

        if (mediaList.isEmpty()) {
            return@withContext getSampleMediaItems()
        }
        mediaList
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".png", true) -> "image/png"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".mp4", true) -> "video/mp4"
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    private fun getSampleFiles(): List<FileNode> {
        val now = System.currentTimeMillis()
        return listOf(
            FileNode("/sdcard/Documents", "Documents", true, 0, now, "folder"),
            FileNode("/sdcard/Download", "Download", true, 0, now, "folder"),
            FileNode("/sdcard/Pictures", "Pictures", true, 0, now, "folder"),
            FileNode("/sdcard/DCIM", "DCIM", true, 0, now, "folder"),
            FileNode("/sdcard/nexus_security_audit.pdf", "nexus_security_audit.pdf", false, 1420500, now - 3600000, "application/pdf"),
            FileNode("/sdcard/device_telemetry_dump.json", "device_telemetry_dump.json", false, 284000, now - 86400000, "application/json")
        )
    }

    private fun getSampleMediaItems(): List<MediaItemData> {
        val now = System.currentTimeMillis()
        return listOf(
            MediaItemData(101, "IMG_20261106_001.jpg", "image/jpeg", 2450000, now - 1800000, "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600", false),
            MediaItemData(102, "VID_20261106_002.mp4", "video/mp4", 18450000, now - 7200000, "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600", true),
            MediaItemData(103, "IMG_20261106_003.jpg", "image/jpeg", 3120000, now - 86400000, "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=600", false)
        )
    }
}
