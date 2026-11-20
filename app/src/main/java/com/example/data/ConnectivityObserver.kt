package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectivityState(
    val isConnected: Boolean = false,
    val networkType: String = "Offline",
    val signalStrengthDbm: Int? = null,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false
)

class ConnectivityObserver(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val deviceId: String
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectivityState = MutableStateFlow(ConnectivityState())
    val connectivityState: StateFlow<ConnectivityState> = _connectivityState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateConnectivityState(network)
        }

        override fun onLost(network: Network) {
            _connectivityState.value = ConnectivityState(isConnected = false, networkType = "Offline")
            syncToFirestore(ConnectivityState(isConnected = false, networkType = "Offline"))
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateConnectivityState(network, networkCapabilities)
        }
    }

    fun startObserving() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)

            // Initial check
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (activeNetwork != null && caps != null) {
                updateConnectivityState(activeNetwork, caps)
            }
        } catch (e: Exception) {
            Log.e("ConnectivityObserver", "Error registering network callback: ${e.message}")
        }
    }

    fun stopObserving() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e("ConnectivityObserver", "Error unregistering network callback: ${e.message}")
        }
    }

    private fun updateConnectivityState(
        network: Network,
        capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
    ) {
        val caps = capabilities ?: return
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        val type = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular 4G/5G"
            else -> "Ethernet"
        }

        val signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            caps.signalStrength
        } else {
            null
        }

        val state = ConnectivityState(
            isConnected = true,
            networkType = type,
            signalStrengthDbm = signalStrength,
            isWifi = isWifi,
            isCellular = isCellular
        )

        _connectivityState.value = state
        syncToFirestore(state)
    }

    private fun syncToFirestore(state: ConnectivityState) {
        scope.launch {
            try {
                firestoreRepository.updateDeviceNetworkState(
                    deviceId = deviceId,
                    networkType = state.networkType,
                    isOnline = state.isConnected
                )
            } catch (e: Exception) {
                Log.e("ConnectivityObserver", "Firestore network sync failed: ${e.message}")
            }
        }
    }
}
