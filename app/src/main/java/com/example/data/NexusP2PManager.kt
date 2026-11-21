package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.AppItem
import com.example.model.DeviceLocation
import com.example.model.FileNode
import com.example.model.NexusDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL

class NexusP2PManager private constructor(private val context: Context) {

    companion object {
        const val DEFAULT_PORT = 8899
        private const val TAG = "NexusP2PManager"

        @Volatile
        private var instance: NexusP2PManager? = null

        fun getInstance(context: Context): NexusP2PManager {
            return instance ?: synchronized(this) {
                instance ?: NexusP2PManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isServerRunning = false

    private val localDeviceManager = LocalDeviceManager(context)
    private val appsRepository = AppsRepository(context)
    private val filesRepository = FilesRepository(context)
    private val locationRepository = LocationRepository(context)

    private val _lastReceivedCommand = MutableStateFlow<String?>(null)
    val lastReceivedCommand: StateFlow<String?> = _lastReceivedCommand.asStateFlow()

    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _pairedRemoteIp = MutableStateFlow<String?>(null)
    val pairedRemoteIp: StateFlow<String?> = _pairedRemoteIp.asStateFlow()

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving local IP", e)
        }
        return "127.0.0.1"
    }

    fun startManagedServer(port: Int = DEFAULT_PORT) {
        if (isServerRunning) return
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isServerRunning = true
                Log.i(TAG, "Managed P2P Server listening on port $port at ${getLocalIpAddress()}")

                while (isServerRunning && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        handleClient(clientSocket)
                    } catch (e: Exception) {
                        if (!isServerRunning) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start P2P Server on port $port", e)
            }
        }
    }

    fun stopServer() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output: OutputStream = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return@launch
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@launch

                val method = parts[0]
                val path = parts[1]

                // Read headers
                var line: String?
                var contentLength = 0
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrBlank()) break
                    if (line!!.lowercase().startsWith("content-length:")) {
                        contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                // Read body if POST
                val body = if (contentLength > 0) {
                    val charArray = CharArray(contentLength)
                    reader.read(charArray, 0, contentLength)
                    String(charArray)
                } else ""

                val (statusCode, responseBody) = processRequest(method, path, body, socket.inetAddress.hostAddress)

                val responseHeader = "HTTP/1.1 $statusCode OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: ${responseBody.toByteArray(Charsets.UTF_8).size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n"

                output.write(responseHeader.toByteArray(Charsets.UTF_8))
                output.write(responseBody.toByteArray(Charsets.UTF_8))
                output.flush()
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client connection", e)
            }
        }
    }

    private suspend fun processRequest(method: String, path: String, body: String, clientIp: String?): Pair<Int, String> {
        return when {
            path.startsWith("/api/ping") -> {
                200 to JSONObject().apply {
                    put("status", "ok")
                    put("role", "MANAGED")
                    put("deviceModel", Build.MODEL)
                    put("manufacturer", Build.MANUFACTURER)
                }.toString()
            }

            path.startsWith("/api/pair") && method == "POST" -> {
                val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val adminId = json.optString("adminDeviceId", "admin_device")
                val adminName = json.optString("adminDeviceName", "Admin Device")

                _isPaired.value = true
                _pairedRemoteIp.value = clientIp

                LiveAlertManager.postAlert(
                    title = "Device Paired!",
                    message = "Connected with Admin: $adminName ($adminId)",
                    type = LiveAlertType.SUCCESS
                )

                200 to JSONObject().apply {
                    put("status", "paired")
                    put("managedDeviceId", "dev_" + Build.ID.take(6))
                    put("managedDeviceName", Build.MODEL)
                }.toString()
            }

            path.startsWith("/api/telemetry") -> {
                val telemetry = localDeviceManager.getLocalDeviceTelemetry("dev_local", Build.MODEL, "MANAGED")
                val json = JSONObject().apply {
                    put("deviceId", telemetry.deviceId)
                    put("deviceName", telemetry.deviceName)
                    put("batteryLevel", telemetry.batteryLevel)
                    put("isCharging", telemetry.isCharging)
                    put("model", telemetry.model)
                    put("manufacturer", telemetry.manufacturer)
                    put("androidVersion", telemetry.androidVersion)
                    put("networkType", telemetry.networkType)
                    put("isOnline", telemetry.isOnline)
                    put("totalStorageGb", telemetry.totalStorageGb)
                    put("usedStorageGb", telemetry.usedStorageGb)
                    put("totalRamGb", telemetry.totalRamGb)
                    put("usedRamGb", telemetry.usedRamGb)
                    put("uptimeHours", telemetry.uptimeHours)
                    put("isLocked", telemetry.isLocked)
                    put("kioskModeActive", telemetry.kioskModeActive)
                }
                200 to json.toString()
            }

            path.startsWith("/api/apps") -> {
                val apps = appsRepository.getInstalledApplications()
                val array = JSONArray()
                for (app in apps) {
                    array.put(JSONObject().apply {
                        put("packageName", app.packageName)
                        put("label", app.label)
                        put("versionName", app.versionName)
                        put("versionCode", app.versionCode)
                        put("isSystemApp", app.isSystemApp)
                        put("installTime", app.installTime)
                        put("usageTimeMillisMs", app.usageTimeMillisMs)
                    })
                }
                200 to array.toString()
            }

            path.startsWith("/api/location") -> {
                val loc = locationRepository.getCurrentLocation()
                val json = JSONObject().apply {
                    put("latitude", loc?.latitude ?: 0.0)
                    put("longitude", loc?.longitude ?: 0.0)
                    put("accuracyMeters", loc?.accuracyMeters ?: 10f)
                    put("timestamp", loc?.timestamp ?: System.currentTimeMillis())
                    put("addressLabel", loc?.addressLabel ?: "Remote Managed Location")
                }
                200 to json.toString()
            }

            path.startsWith("/api/files") -> {
                val nodes = filesRepository.listDirectory("/sdcard")
                val array = JSONArray()
                for (file in nodes) {
                    array.put(JSONObject().apply {
                        put("name", file.name)
                        put("path", file.path)
                        put("isDirectory", file.isDirectory)
                        put("sizeBytes", file.sizeBytes)
                        put("mimeType", file.mimeType)
                    })
                }
                200 to array.toString()
            }

            path.startsWith("/api/command") && method == "POST" -> {
                val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val command = json.optString("command", "UNKNOWN")
                _lastReceivedCommand.value = command

                LiveAlertManager.postAlert(
                    title = "Remote Command Received",
                    message = "Admin dispatched command: $command",
                    type = LiveAlertType.WARNING
                )

                200 to JSONObject().apply {
                    put("status", "executed")
                    put("command", command)
                    put("timestamp", System.currentTimeMillis())
                }.toString()
            }

            else -> 404 to "{\"error\":\"Not Found\"}"
        }
    }

    // ==========================================
    // ADMIN CLIENT CALLS (Executed on Admin phone)
    // ==========================================

    suspend fun pairWithManagedHost(
        managedIp: String,
        managedPort: Int = DEFAULT_PORT,
        adminDeviceId: String,
        adminDeviceName: String
    ): Result<NexusDevice> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$managedIp:$managedPort/api/pair")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("adminDeviceId", adminDeviceId)
                put("adminDeviceName", adminDeviceName)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == 200) {
                // Now fetch telemetry
                val telemetryResult = fetchRemoteTelemetry(managedIp, managedPort)
                if (telemetryResult.isSuccess) {
                    Result.success(telemetryResult.getOrThrow())
                } else {
                    Result.success(
                        NexusDevice(
                            deviceId = "p2p_managed_" + managedIp.replace(".", "_"),
                            deviceName = "Managed Device ($managedIp)",
                            ipAddress = managedIp,
                            p2pPort = managedPort,
                            isOnline = true
                        )
                    )
                }
            } else {
                Result.failure(Exception("Managed device returned HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteTelemetry(ip: String, port: Int = DEFAULT_PORT): Result<NexusDevice> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:$port/api/telemetry")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val device = NexusDevice(
                    deviceId = json.optString("deviceId", "dev_$ip"),
                    deviceName = json.optString("deviceName", "Remote Managed Device"),
                    model = json.optString("model", "Android Device"),
                    manufacturer = json.optString("manufacturer", "Generic"),
                    androidVersion = json.optString("androidVersion", "Android 14"),
                    batteryLevel = json.optInt("batteryLevel", 80),
                    isCharging = json.optBoolean("isCharging", false),
                    networkType = json.optString("networkType", "Wi-Fi"),
                    isOnline = true,
                    totalStorageGb = json.optDouble("totalStorageGb", 128.0),
                    usedStorageGb = json.optDouble("usedStorageGb", 42.0),
                    totalRamGb = json.optDouble("totalRamGb", 8.0),
                    usedRamGb = json.optDouble("usedRamGb", 3.2),
                    uptimeHours = json.optLong("uptimeHours", 10),
                    isLocked = json.optBoolean("isLocked", false),
                    kioskModeActive = json.optBoolean("kioskModeActive", false),
                    ipAddress = ip,
                    p2pPort = port,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
                Result.success(device)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteApps(ip: String, port: Int = DEFAULT_PORT): Result<List<AppItem>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:$port/api/apps")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(response)
                val list = mutableListOf<AppItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AppItem(
                            packageName = obj.optString("packageName"),
                            label = obj.optString("label", "App"),
                            versionName = obj.optString("versionName", "1.0"),
                            versionCode = obj.optLong("versionCode", 1L),
                            isSystemApp = obj.optBoolean("isSystemApp"),
                            installTime = obj.optLong("installTime", System.currentTimeMillis()),
                            usageTimeMillisMs = obj.optLong("usageTimeMillisMs", 0L)
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRemoteLocation(ip: String, port: Int = DEFAULT_PORT): Result<DeviceLocation> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:$port/api/location")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val loc = DeviceLocation(
                    latitude = json.optDouble("latitude", 0.0),
                    longitude = json.optDouble("longitude", 0.0),
                    accuracyMeters = json.optDouble("accuracyMeters", 10.0).toFloat(),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    addressLabel = json.optString("addressLabel", "Remote Managed Location")
                )
                Result.success(loc)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendRemoteCommand(ip: String, command: String, port: Int = DEFAULT_PORT): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:$port/api/command")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("command", command)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            if (conn.responseCode == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
