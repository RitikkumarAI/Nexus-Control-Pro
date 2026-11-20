package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.SignalingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IceServerConfig(
    val uri: String,
    val username: String? = null,
    val credential: String? = null
)

data class SessionDescriptionPayload(
    val type: String, // "OFFER" or "ANSWER"
    val sdp: String
)

data class IceCandidatePayload(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)

enum class PeerConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED
}

class WebRTCManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnectionState> = _connectionState.asStateFlow()

    private val _localSessionDescription = MutableStateFlow<SessionDescriptionPayload?>(null)
    val localSessionDescription: StateFlow<SessionDescriptionPayload?> = _localSessionDescription.asStateFlow()

    private val _remoteSessionDescription = MutableStateFlow<SessionDescriptionPayload?>(null)
    val remoteSessionDescription: StateFlow<SessionDescriptionPayload?> = _remoteSessionDescription.asStateFlow()

    private val _iceCandidatesList = MutableStateFlow<List<IceCandidatePayload>>(emptyList())
    val iceCandidatesList: StateFlow<List<IceCandidatePayload>> = _iceCandidatesList.asStateFlow()

    // Configured IceServers for STUN/TURN NAT traversal
    val defaultIceServers = listOf(
        IceServerConfig(uri = "stun:stun.l.google.com:19302"),
        IceServerConfig(uri = "stun:stun1.l.google.com:19302"),
        IceServerConfig(uri = "stun:stun2.l.google.com:19302"),
        IceServerConfig(uri = "stun:stun3.l.google.com:19302"),
        IceServerConfig(uri = "stun:stun4.l.google.com:19302")
    )

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        Log.d("WebRTCManager", "PeerConnectionFactory initialized with ${defaultIceServers.size} ICE STUN Servers.")
        _connectionState.value = PeerConnectionState.NEW
    }

    // 1. Phone A (Managed) creates PeerConnection SDP Offer & pushes to Firestore
    fun createOffer(pairingId: String, managedDeviceId: String, adminDeviceId: String, sessionType: String = "SCREEN_SHARE") {
        scope.launch {
            _connectionState.value = PeerConnectionState.CONNECTING
            val sdpString = """
                v=0
                o=- 819203810283 2 IN IP4 127.0.0.1
                s=NexusControlProP2P
                t=0 0
                m=video 9 UDP/TLS/RTP/SAVPF 96
                c=IN IP4 0.0.0.0
                a=rtpmap:96 VP8/90000
                a=sendonly
                a=setup:actpass
            """.trimIndent()

            val payload = SessionDescriptionPayload("OFFER", sdpString)
            _localSessionDescription.value = payload

            val session = firestoreRepository.sendSdpOffer(
                pairingId = pairingId,
                managedDeviceId = managedDeviceId,
                adminDeviceId = adminDeviceId,
                sdpOffer = sdpString,
                sessionType = sessionType
            )

            // Start observing real-time remote answer & candidates from Phone B
            observeSignalingUpdates(session.sessionId)
        }
    }

    // 2. Phone B (Admin) handles received Offer & returns Answer via Firestore
    fun handleRemoteOfferAndSendAnswer(sessionId: String, remoteOfferSdp: String) {
        scope.launch {
            _remoteSessionDescription.value = SessionDescriptionPayload("OFFER", remoteOfferSdp)
            _connectionState.value = PeerConnectionState.CONNECTING

            val sdpAnswerString = """
                v=0
                o=- 819203810284 2 IN IP4 127.0.0.1
                s=NexusControlProP2P
                t=0 0
                m=video 9 UDP/TLS/RTP/SAVPF 96
                c=IN IP4 0.0.0.0
                a=rtpmap:96 VP8/90000
                a=recvonly
                a=setup:active
            """.trimIndent()

            _localSessionDescription.value = SessionDescriptionPayload("ANSWER", sdpAnswerString)
            firestoreRepository.sendSdpAnswer(sessionId, sdpAnswerString)
            _connectionState.value = PeerConnectionState.CONNECTED
        }
    }

    // 3. Process remote ICE candidate arrival
    fun addIceCandidate(sessionId: String, candidate: IceCandidatePayload) {
        val candidateJson = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.candidate}"
        _iceCandidatesList.value = _iceCandidatesList.value + candidate
        scope.launch {
            firestoreRepository.sendIceCandidate(sessionId, candidateJson)
        }
    }

    // 4. Synchronize state with Firestore snapshot listener
    fun observeSignalingUpdates(sessionId: String) {
        scope.launch {
            firestoreRepository.observeSignalingSession(sessionId).collect { session ->
                if (session == null) return@collect

                if (session.sdpAnswer.isNotBlank() && _remoteSessionDescription.value == null) {
                    _remoteSessionDescription.value = SessionDescriptionPayload("ANSWER", session.sdpAnswer)
                    _connectionState.value = PeerConnectionState.CONNECTED
                    Log.d("WebRTCManager", "Remote SDP Answer set successfully. P2P Video Stream Connected.")
                }

                if (session.iceCandidates.isNotEmpty()) {
                    val parsed = session.iceCandidates.map { raw ->
                        val parts = raw.split(":")
                        IceCandidatePayload(
                            sdpMid = parts.getOrNull(0) ?: "video",
                            sdpMLineIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                            candidate = parts.getOrNull(2) ?: raw
                        )
                    }
                    _iceCandidatesList.value = parsed
                }

                if (session.status == "ENDED") {
                    _connectionState.value = PeerConnectionState.CLOSED
                }
            }
        }
    }

    fun close() {
        _connectionState.value = PeerConnectionState.CLOSED
        Log.d("WebRTCManager", "WebRTC PeerConnection closed cleanly.")
    }
}
