package com.velitosoft.ayame_sdk.internal

import android.content.Context
import android.util.Log
import com.velitosoft.ayame_sdk.AudioController
import com.velitosoft.ayame_sdk.AyameOptions
import com.velitosoft.ayame_sdk.VideoController
import com.velitosoft.ayame_sdk.internal.media.LocalAudioManager
import com.velitosoft.ayame_sdk.internal.media.LocalVideoManager
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import java.util.UUID

internal class PeerChannel(
    private val context: Context,
    private val options: AyameOptions,
    private val listener: Listener,
    private val customVideoCapturer: VideoCapturer? = null
) {
    interface Listener {
        fun onSetLocalDescriptionSuccess(sdp: SessionDescription)
        fun onIceCandidate(candidate: IceCandidate)
        fun onAddStream(stream: MediaStream)
        fun onRemoveStream(stream: MediaStream)
        fun onIceConnectionChange(state: PeerConnection.IceConnectionState)
        fun onError(description: String)
    }

    val rootEglBase: EglBase = EglBase.create()

    private val factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    private val localAudioManager: LocalAudioManager
    private val localVideoManager: LocalVideoManager

    val videoController: VideoController
        get() = localVideoManager
    val audioController: AudioController
        get() = localAudioManager


    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )

        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))

        // TODO(zztkm): AyameOptionsに基づいて builder を設定する
        factory = builder.createPeerConnectionFactory()

        localAudioManager = LocalAudioManager(factory, options)
        localVideoManager = LocalVideoManager(
            context,
            factory,
            rootEglBase.eglBaseContext,
            options,
            customVideoCapturer
        )
    }

    fun startLocalStream() {
        localAudioManager.start()
        localVideoManager.start()

        if (localAudioManager.track != null || localVideoManager.track != null) {
            val localStream = factory.createLocalMediaStream(UUID.randomUUID().toString())
            localAudioManager.track?.let {
                localStream.addTrack(it)
            }
            localVideoManager.track?.let {
                localStream.addTrack(it)
            }
            listener.onAddStream(localStream)
        }
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
            }

            override fun onRenegotiationNeeded() {
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                if (state != null) {
                    listener.onIceConnectionChange(state)
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
            }
        })

        // PeerConnection にトラックを追加
        localAudioManager.track?.let {
            peerConnection?.addTrack(it, listOf(it.id()))
        }
        localVideoManager.track?.let {
            peerConnection?.addTrack(it, listOf(it.id()))
        }
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
            override fun onSetFailure(error: String?) {
                listener.onError("Set local description failed: $error")
            }
            override fun onCreateSuccess(sdp: SessionDescription?) { }
            override fun onCreateFailure(error: String?) { }
        }, sdp)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.i("Ayame", "setRemoteDescription: success")
            }
            override fun onSetFailure(error: String?) {
                listener.onError("Set remote description failed: $error")
            }
            override fun onCreateSuccess(sdp: SessionDescription?) { }
            override fun onCreateFailure(error: String?) { }

        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun close() {
        localAudioManager.stop()
        localVideoManager.stop()

        peerConnection?.close()
        peerConnection = null

        rootEglBase.release()
    }
}
