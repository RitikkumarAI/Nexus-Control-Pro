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

enum class WebRtcState {
    DISCONNECTED,
    CREATING_OFFER,
    AWAITING_ANSWER,
    EXCHANGING_ICE,
    CONNECTED,
    CLOSED
}

class WebRtcSignalingEngine(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(WebRtcState.DISCONNECTED)
    val connectionState: StateFlow<WebRtcState> = _connectionState.asStateFlow()

    private val _activeSession = MutableStateFlow<SignalingSession?>(null)
    val activeSession: StateFlow<SignalingSession?> = _activeSession.asStateFlow()

    private val _receivedIceCandidates = MutableStateFlow<List<String>>(emptyList())
    val receivedIceCandidates: StateFlow<List<String>> = _receivedIceCandidates.asStateFlow()

    // 1. Phone A (Managed) initiates WebRTC screen capture offer
    fun createOfferAndPublish(
        pairingId: String,
        managedDeviceId: String,
        adminDeviceId: String,
        sessionType: String = "SCREEN_SHARE"
    ) {
        scope.launch {
            _connectionState.value = WebRtcState.CREATING_OFFER
            val dummySdpOffer = """
                v=0
                o=- 718392018239 2 IN IP4 127.0.0.1
                s=NexusControlProWebRTC
                t=0 0
                m=video 9 UDP/TLS/RTP/SAVPF 96
                c=IN IP4 0.0.0.0
                a=rtpmap:96 VP8/90000
                a=setup:actpass
            """.trimIndent()

            val session = firestoreRepository.sendSdpOffer(
                pairingId = pairingId,
                managedDeviceId = managedDeviceId,
                adminDeviceId = adminDeviceId,
                sdpOffer = dummySdpOffer,
                sessionType = sessionType
            )
            _activeSession.value = session
            _connectionState.value = WebRtcState.AWAITING_ANSWER

            // Listen for Phone B's SDP answer & ICE candidates in real-time
            listenToSignalingExchange(session.sessionId)
        }
    }

    // 2. Phone B (Admin) receives offer and publishes SDP answer
    fun receiveOfferAndPublishAnswer(sessionId: String) {
        scope.launch {
            _connectionState.value = WebRtcState.AWAITING_ANSWER
            val dummySdpAnswer = """
                v=0
                o=- 718392018240 2 IN IP4 127.0.0.1
                s=NexusControlProWebRTC
                t=0 0
                m=video 9 UDP/TLS/RTP/SAVPF 96
                c=IN IP4 0.0.0.0
                a=rtpmap:96 VP8/90000
                a=setup:active
            """.trimIndent()

            firestoreRepository.sendSdpAnswer(sessionId, dummySdpAnswer)
            _connectionState.value = WebRtcState.EXCHANGING_ICE
        }
    }

    // 3. Publish STUN/TURN ICE Candidates to Firestore
    fun addIceCandidate(sessionId: String, candidateSdp: String) {
        scope.launch {
            firestoreRepository.sendIceCandidate(sessionId, candidateSdp)
        }
    }

    // 4. Listen to real-time updates from Firestore Signaling collection
    private fun listenToSignalingExchange(sessionId: String) {
        scope.launch {
            firestoreRepository.observeSignalingSession(sessionId).collect { session ->
                if (session == null) return@collect
                _activeSession.value = session

                if (session.sdpAnswer.isNotBlank() && _connectionState.value == WebRtcState.AWAITING_ANSWER) {
                    Log.d("WebRtcSignalingEngine", "Received SDP Answer from Admin Device!")
                    _connectionState.value = WebRtcState.EXCHANGING_ICE
                }

                if (session.iceCandidates.isNotEmpty()) {
                    _receivedIceCandidates.value = session.iceCandidates
                    if (session.status == "CONNECTED") {
                        _connectionState.value = WebRtcState.CONNECTED
                    }
                }

                if (session.status == "ENDED") {
                    _connectionState.value = WebRtcState.CLOSED
                }
            }
        }
    }

    fun closeSession() {
        val session = _activeSession.value
        if (session != null) {
            scope.launch {
                firestoreRepository.endSignalingSession(session.sessionId)
                _connectionState.value = WebRtcState.CLOSED
            }
        }
    }
}
