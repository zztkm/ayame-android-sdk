package com.velitosoft.ayame_sdk.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed class AyameSignalingMessage {
    abstract val type: String
}

/// --------------------------------------
// 送信メッセージ (Client -> Server)
// --------------------------------------

@Serializable
@SerialName("register")
data class RegisterMessage(
    override val type: String = "register",
    val roomId: String,
    val clientId: String,
    val signalingKey: String? = null,
    val authnMetadata: JsonElement? = null,
) : AyameSignalingMessage()

@Serializable
@SerialName("offer")
data class OfferMessage(
    override val type: String = "offer",
    val sdp: String
) : AyameSignalingMessage()

@Serializable
@SerialName("answer")
data class AnswerMessage(
    override val type: String = "answer",
    val sdp: String
) : AyameSignalingMessage()

@Serializable
@SerialName("candidate")
data class CandidateMessage(
    override val type: String = "candidate",
    val ice: IceDto
) : AyameSignalingMessage()

@Serializable
@SerialName("pong")
data class PongMessage(
    override val type: String = "pong"
) : AyameSignalingMessage()

@Serializable
@SerialName("bye")
data class ByeMessage(
    override val type: String = "bye"
) : AyameSignalingMessage()

// --------------------------------------
// 受信メッセージ (Server -> Client)
// --------------------------------------

@Serializable
@SerialName("accept")
data class AcceptMessage(
    override val type: String = "accept",
    val authzMetadata: JsonElement,
    val iceServers: List<IceServer>,
    // isExistUser と isExistClient は基本どちらしか入ってこないため、nullable にしている
    // 過去の Ayame では isExistUser が利用されていたが、現在は isExistClient が利用されているという経緯がある
    val isExistClient: Boolean? = null,
    val isExistUser: Boolean? = null,
) : AyameSignalingMessage()

@Serializable
@SerialName("reject")
data class RejectMessage(
    override val type: String = "reject",
    val reason: String,
) : AyameSignalingMessage()

@Serializable
@SerialName("ping")
data class PingMessage(
    override val type: String = "ping"
) : AyameSignalingMessage()

// --------------------------------------
// 補助データクラス
// --------------------------------------

// STUN / TURN サーバー情報
// refs: https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/RTCPeerConnection#iceservers
@Serializable
data class IceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,

    // NOTE: tlsCertPolicy は Web ブラウザの Ayame Client からは送信されないためコメントアウト
    //val tlsCertPolicy: PeerConnection.TlsCertPolicy
)

/* ICE candidate 情報
*
* NOTE: https://developer.mozilla.org/en-US/docs/Web/API/RTCIceCandidate/RTCIceCandidate を見ると
*       candidate, sdpMid, sdpMLineIndex, usernameFragment の4つのプロパティがあるが、
*       MDN のドキュメント上はすべて Optional となっていた。
*       しかし、 libwebrtc (android) の IceCandidate クラスのコンストラクタを見ると
*       candidate, sdpMid, sdpMLineIndex は必須であったため、 IceDto ではそれに合わせている。
*/
@Serializable
data class IceDto(
    val candidate: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,

    // NOTE: MDN のドキュメント上は存在するが libwebrtc (android) の
    //       IceCandidate クラスのコンストラクタには無いためコメントアウトしている
    // val usernameFragment: String? = null
)