package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceNetworkStatus(
    val isConnected: Boolean = false,
    val networkType: String = "Offline",
    val isWifi: Boolean = false,
    val isCellular: Boolean = false
)

class NetworkManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository = FirestoreRepository(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow(DeviceNetworkStatus())
    val networkState: StateFlow<DeviceNetworkStatus> = _networkState.asStateFlow()

    private var targetDeviceId: String = "phone_b"

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateNetworkStatus(network)
        }

        override fun onLost(network: Network) {
            val offline = DeviceNetworkStatus(isConnected = false, networkType = "Offline")
            _networkState.value = offline
            syncToFirestore(offline)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateNetworkStatus(network, networkCapabilities)
        }
    }

    fun startMonitoring(deviceId: String = "phone_b") {
        this.targetDeviceId = deviceId
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)

            // Initial status query
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                updateNetworkStatus(activeNetwork)
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to start monitoring: ${e.message}")
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to stop monitoring: ${e.message}")
        }
    }

    private fun updateNetworkStatus(
        network: Network,
        capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
    ) {
        val caps = capabilities ?: return
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val networkType = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular 4G/5G"
            else -> "Ethernet"
        }

        val status = DeviceNetworkStatus(
            isConnected = true,
            networkType = networkType,
            isWifi = isWifi,
            isCellular = isCellular
        )

        _networkState.value = status
        syncToFirestore(status)
    }

    private fun syncToFirestore(status: DeviceNetworkStatus) {
        scope.launch {
            try {
                firestoreRepository.updateDeviceNetworkState(
                    deviceId = targetDeviceId,
                    networkType = status.networkType,
                    isOnline = status.isConnected
                )
            } catch (e: Exception) {
                Log.e("NetworkManager", "Firestore sync error: ${e.message}")
            }
        }
    }
}
