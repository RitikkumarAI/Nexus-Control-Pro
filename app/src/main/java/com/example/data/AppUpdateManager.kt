package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val isMandatory: Boolean = false
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

class AppUpdateManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val GITHUB_REPO = "RitikkumarAI/Nexus-Control-Pro"
        private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/$GITHUB_REPO/main/version.json"
        private const val GITHUB_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private var dismissedVersionCode: Int = 0

    fun checkForUpdatesSilently() {
        scope.launch {
            val update = checkForUpdates()
            if (update != null && update.versionCode <= dismissedVersionCode) {
                _updateInfo.value = null
            }
        }
    }

    suspend fun checkForUpdates(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        _isChecking.value = true
        try {
            val currentCode = BuildConfig.VERSION_CODE
            // Check version.json from raw GitHub
            val timestamp = System.currentTimeMillis()
            val url = URL("$VERSION_JSON_URL?t=$timestamp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val remoteCode = json.optInt("versionCode", 1)
                val remoteName = json.optString("versionName", "1.0")
                val notes = json.optString("releaseNotes", "Bug fixes and performance improvements.")
                val apkUrl = json.optString("apkUrl", "")

                Log.d(TAG, "Current versionCode: $currentCode, Remote versionCode: $remoteCode")

                if (remoteCode > currentCode && apkUrl.isNotBlank()) {
                    val info = AppUpdateInfo(
                        versionCode = remoteCode,
                        versionName = remoteName,
                        releaseNotes = notes,
                        apkUrl = apkUrl,
                        isMandatory = json.optBoolean("isMandatory", false)
                    )
                    if (remoteCode > dismissedVersionCode) {
                        _updateInfo.value = info
                    }
                    _isChecking.value = false
                    return@withContext info
                } else {
                    _updateInfo.value = null
                    _isChecking.value = false
                    return@withContext null
                }
            }

            // Fallback: check GitHub Releases API
            val releaseConn = (URL(GITHUB_RELEASE_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "NexusControlPro-App")
            }

            if (releaseConn.responseCode == 200) {
                val releaseStr = releaseConn.inputStream.bufferedReader().use { it.readText() }
                val releaseJson = JSONObject(releaseStr)
                val tagName = releaseJson.optString("tag_name", "").removePrefix("v")
                val releaseNotes = releaseJson.optString("body", "New version available on GitHub.")
                val assets = releaseJson.optJSONArray("assets")

                var apkDownloadUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                if (!apkDownloadUrl.isNullOrBlank()) {
                    val remoteVersionCode = tagName.filter { it.isDigit() }.toIntOrNull() ?: 2
                    if (remoteVersionCode > BuildConfig.VERSION_CODE) {
                        val info = AppUpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = tagName,
                            releaseNotes = releaseNotes,
                            apkUrl = apkDownloadUrl
                        )
                        _updateInfo.value = info
                        _isChecking.value = false
                        return@withContext info
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed or device is offline: ${e.message}")
        } finally {
            _isChecking.value = false
        }
        null
    }

    fun startDownload(apkUrl: String) {
        scope.launch {
            downloadAndPrepareInstall(apkUrl)
        }
    }

    private suspend fun downloadAndPrepareInstall(apkUrl: String): Unit = withContext(Dispatchers.IO) {
        _downloadState.value = UpdateDownloadState.Downloading(0.01f)
        try {
            val url = URL(apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 20000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }

            if (connection.responseCode in 300..399) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrBlank()) {
                    downloadAndPrepareInstall(newUrl)
                    return@withContext
                }
            }

            val totalBytes = connection.contentLengthLong
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val outputFile = File(downloadsDir, "NexusControlPro_Update.apk")

            if (outputFile.exists()) {
                outputFile.delete()
            }

            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                if (totalBytes > 0) {
                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    _downloadState.value = UpdateDownloadState.Downloading(progress)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            _downloadState.value = UpdateDownloadState.ReadyToInstall(outputFile)

            // Trigger System Package Installer
            withContext(Dispatchers.Main) {
                installApk(outputFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "APK Download failed", e)
            _downloadState.value = UpdateDownloadState.Error("Download failed: ${e.localizedMessage}")
        }
    }

    fun installApk(file: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            _downloadState.value = UpdateDownloadState.Error("Install launch failed: ${e.localizedMessage}")
        }
    }

    fun dismissUpdate() {
        val current = _updateInfo.value
        if (current != null) {
            dismissedVersionCode = current.versionCode
        }
        _updateInfo.value = null
        _downloadState.value = UpdateDownloadState.Idle
    }
}
