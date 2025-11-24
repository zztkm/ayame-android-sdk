package com.velitosoft.ayame_sdk.internal

import android.content.Context
import android.util.Log
import com.velitosoft.ayame_sdk.AyameOptions
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

internal class PeerChannel(
    private val context: Context,
    private val options: AyameOptions,
    private val listener: Listener
) {
    interface Listener {
        fun onSetLocalDescriptionSuccess(sdp: SessionDescription)
        fun onIceCandidate(candidate: IceCandidate)
        fun onAddStream(stream: MediaStream)
        fun onRemoveStream(stream: MediaStream)
        fun onIceConnectionChange(state: PeerConnection.IceConnectionState)
        fun onError(description: String)
    }

    private val factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val builder = PeerConnectionFactory.builder()

        // TODO(zztkm): AyameOptionsに基づいて builder を設定する

        factory = builder.createPeerConnectionFactory()
    }

    fun createPeerConnection(iceServers: List<PeerConnection.IceServer>) {
        val rtcConfig = PeerConnection.RTCConfiguration(
            iceServers.map {
                PeerConnection.IceServer.builder(it.urls)
                    .setUsername(it.username)
                    .setPassword(it.password)
                    .createIceServer()
            }
        )
        // TODO(zztkm): Gemini 3 がおすすめしてきたがなんでおすすめか調べる
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    listener.onIceCandidate(candidate)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate?>?) {
                TODO("Not yet implemented")
            }

            override fun onAddStream(stream: MediaStream?) {
                if (stream != null) {
                    listener.onAddStream(stream)
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                if (stream != null) {
                    listener.onRemoveStream(stream)
                }
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                TODO("Not yet implemented")
            }

            override fun onRenegotiationNeeded() {
                TODO("Not yet implemented")
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                TODO("Not yet implemented")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state != null) {
                    listener.onIceConnectionChange(state)
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                TODO("Not yet implemented")
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun createOffer() {
        val constraints = MediaConstraints()

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp != null) {
                    setLocalDescription(sdp)
                }
            }

            override fun onSetSuccess() { }
            override fun onCreateFailure(error: String?) {
                listener.onError("Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) { }
        }, constraints)
    }

    fun createAnswer() {
        val constraints = MediaConstraints()

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d("Ayame", "createAnswer: success")
                if (sdp != null) {
                    setLocalDescription(sdp)
                }
            }

            override fun onSetSuccess() { }
            override fun onCreateFailure(error: String?) {
                listener.onError("Create answer failed: $error")
            }
            override fun onSetFailure(error: String?) { }
        }, constraints)
    }

    private fun setLocalDescription(sdp: SessionDescription) {
        peerConnection?.setLocalDescription(object : SdpObserver {
            override fun onSetSuccess() {
                listener.onSetLocalDescriptionSuccess(sdp)
            }

            override fun onCreateSuccess(sdp: SessionDescription?) { }
            override fun onCreateFailure(error: String?) { }
            override fun onSetFailure(error: String?) {
                listener.onError("Set local description failed: $error")
            }
        }, sdp)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.i("Ayame", "setRemoteDescription: success")
            }

            override fun onCreateSuccess(sdp: SessionDescription?) { }

            override fun onCreateFailure(error: String?) { }

            override fun onSetFailure(error: String?) {
                listener.onError("Set remote description failed: $error")
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun close() {
        peerConnection?.close()
        peerConnection = null
    }
}
