package com.velitosoft.ayame_sdk

import android.content.Context
import android.util.Log
import com.velitosoft.ayame_sdk.internal.AcceptMessage
import com.velitosoft.ayame_sdk.internal.AnswerMessage
import com.velitosoft.ayame_sdk.internal.OfferMessage
import com.velitosoft.ayame_sdk.internal.PeerChannel
import com.velitosoft.ayame_sdk.internal.RejectMessage
import com.velitosoft.ayame_sdk.internal.SignalingChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import java.util.UUID

class AyameMediaChannel(
    private val signalingUrl: String,
    private val roomId: String,
    private val context: Context,
    private val options: AyameOptions = AyameOptions.default(),
    private val clientId: String = UUID.randomUUID().toString(),
    private val signalingKey: String? = null,
    private val authnMetadata: String? = null,
    customVideoCapturer: VideoCapturer? = null,
) {
    var listener: AyameListener? = null

    private val signaling = SignalingChannel(
        signalingUrl,
        roomId,
        clientId,
        signalingKey,
        authnMetadata
    )
    private val peer = PeerChannel(context, options, createPeerChannelListener(), customVideoCapturer)

    val videoController: VideoController
        get() = peer.videoController
    val audioController: AudioController
        get() = peer.audioController
    val eglBaseContext: EglBase.Context
        get() = peer.rootEglBase.eglBaseContext

    init {
        signaling.listener = createSignalingListener()
    }

    fun connect() {
        peer.startLocalStream()
        signaling.connect()
    }

    fun disconnect() {
        signaling.disconnect()
        peer.close()
        // NOTE: onDisconnected は SignalingChannel と PeerChannel の
        //       どちらかが閉じたときに呼ばれるのでここでは呼ばない
    }

    private fun handleAccept(message: AcceptMessage) {
        // PeerConnection の初期化
        // org.webrtc.PeerConnection.IceServer のリストに変換する
        val iceServers = message.iceServers.map {
            PeerConnection.IceServer.builder(it.urls)
                .setUsername(it.username)
                .setPassword(it.credential)
                .createIceServer()
        }
        peer.createPeerConnection(iceServers)

        // isExistUser or isExistClient が true の場合には offer を送信する
        // そうでない場合は相手からの offer を待つ
        if (message.isExistUser?: false || message.isExistClient?: false) {
            // offer を作成したら PeerChannel 内部で setLocalDescription が呼ばれる
            // その後 onSetLocalDescriptionSuccess が呼ばれるのでそこで offer を送信する
            peer.createOffer()
        }
    }

    private fun createSignalingListener(): SignalingChannel.Listener {
        return object : SignalingChannel.Listener {
            override fun onAccept(message: AcceptMessage) {
                handleAccept(message)
            }

            override fun onReject(message: RejectMessage) {
                // TODO: エラーの原因を判別しやすいように Error 種別の enum を持たせる
                listener?.onError("Signaling rejected: ${message.reason}")
            }

            override fun onOffer(message: OfferMessage) {
                val sdp = SessionDescription(
                    SessionDescription.Type.OFFER,
                    message.sdp
                )
                peer.setRemoteDescription(sdp)
                // answer を作成したら PeerChannel 内部で setLocalDescription が呼ばれる
                // その後 onSetLocalDescriptionSuccess が呼ばれるのでそこで answer を送信する
                peer.createAnswer()
            }

            override fun onAnswer(message: AnswerMessage) {
                val sdp = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.sdp
                )
                //
                peer.setRemoteDescription(sdp)
            }

            override fun onCandidate(message: IceCandidate) {
                // ここで PeerConnection に ICE candidate を追加する
            }

            override fun onClosed() {
                listener?.onDisconnected("Signaling channel closed")
            }

            override fun onError(reason: String) {
                listener?.onError("Signaling error: $reason")
            }
        }
    }

    private fun createPeerChannelListener(): PeerChannel.Listener {
        return object : PeerChannel.Listener {
            override fun onSetLocalDescriptionSuccess(sdp: SessionDescription) {
                when (sdp.type) {
                    SessionDescription.Type.OFFER -> {
                        signaling.sendOffer(sdp.description)
                    }
                    SessionDescription.Type.ANSWER -> {
                        signaling.sendAnswer(sdp.description)
                    }
                    else -> Log.w("Ayame", "Unknown SDP type: ${sdp.type}")
                }
            }

            override fun onIceCandidate(candidate: IceCandidate) {
                // PeerChannel から ICE candidate を受け取ったら相手に送信する
                signaling.sendCandidate(candidate)
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d("Ayame", "ICE Connection State changed: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        listener?.onConnected()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.CLOSED -> {
                        listener?.onDisconnected("ICE connection state: $state")
                    }

                    // 何もしない状態
                    PeerConnection.IceConnectionState.NEW,
                    PeerConnection.IceConnectionState.CHECKING -> {
                        Log.d("Ayame", "ICE connection in progress: $state")
                    }
                }
            }

            override fun onAddStream(stream: MediaStream) {
                listener?.onAddRemoteStream(stream)
            }

            override fun onRemoveStream(stream: MediaStream) {
                listener?.onRemoveRemoteStream(stream)
            }

            override fun onError(description: String) {
                listener?.onError(description)
            }
        }
    }
}