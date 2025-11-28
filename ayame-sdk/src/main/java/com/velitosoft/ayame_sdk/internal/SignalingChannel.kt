package com.velitosoft.ayame_sdk.internal

import kotlinx.serialization.json.Json
import java.util.UUID
import android.util.Log
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.webrtc.IceCandidate

/**
 * Ayame のシグナリングを管理するクラス。
 *
 * @param signalingUrl シグナリングサーバーのURL
 * @param roomId ルームID
 * @param clientId クライアントID（指定がなければランダム生成）
 * @param signalingKey シグナリングキー（オプション）
 * @param authnMetadata 認証メタデータ（オプション）、JSON 文字列を期待
 */
class SignalingChannel(
    private val signalingUrl: String,
    private val roomId: String,
    private val clientId: String = UUID.randomUUID().toString(),
    private val signalingKey: String? = null,
    private val authnMetadata: String? = null,
) {
    private val client = OkHttpClient()
    private var ws: WebSocket? = null

    // JSONの設定（未知のキーがあっても無視するように設定）
    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    // 外部へのイベント通知用リスナー
    interface Listener {
        fun onAccept(message: AcceptMessage)
        fun onReject(message: RejectMessage)
        fun onOffer(message: OfferMessage)
        fun onAnswer(message: AnswerMessage)
        fun onCandidate(message: IceCandidate)
        fun onClosed()
        fun onError(reason: String)
    }

    var listener: Listener? = null

    fun connect() {
        val request = Request.Builder()
            .url(signalingUrl)
            .build()

        // WebSocketリスナーの定義
        val webSocketListener = object : WebSocketListener() {

            // 1. 接続確立 -> すぐに register を送信
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("Ayame", "WebSocket Connected. Sending register...")
                sendRegister(webSocket)
            }

            // 2. メッセージ受信 -> accept かどうか判定
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("Ayame", "Received: $text")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("Ayame", "Closing: $reason")
                webSocket.close(1000, null)
                listener?.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("Ayame", "Error: ${t.message}, jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj")
                listener?.onError(t.message ?: "Unknown WebSocket error")
            }
        }

        // 接続開始
        ws = client.newWebSocket(request, webSocketListener) }

    fun disconnect() {
        sendBye()
        ws?.close(1000, "Bye")
        ws = null
    }

    // "register" メッセージの作成と送信
    private fun sendRegister(ws: WebSocket) {
        val authnMetadataElement = authnMetadata?.let {
            try {
                jsonFormat.parseToJsonElement(it)
            } catch (e: Exception) {
                Log.w("Ayame", "Failed to parse: $it, error: ${e.message}")
                null
            }
        }
        val msg = RegisterMessage(
            roomId = roomId,
            clientId = clientId,
            signalingKey = signalingKey,
            authnMetadata = authnMetadataElement,
        )
        sendJson(msg)
    }

    fun sendOffer(sdp: String) {
        val msg = OfferMessage(sdp = sdp)
        sendJson(msg)
    }

    fun sendAnswer(sdp: String) {
        val msg = AnswerMessage(sdp = sdp)
        sendJson(msg)
    }

    fun sendCandidate(ice: IceCandidate) {
        val iceDto = IceDto(
            candidate = ice.sdp,
            sdpMid = ice.sdpMid,
            sdpMLineIndex = ice.sdpMLineIndex,
        )
        val msg = CandidateMessage(ice = iceDto)
        sendJson(msg)
    }

    private fun sendPong() {
        val msg = PongMessage()
        sendJson(msg)
    }

    private fun sendBye() {
        try {
            val text = jsonFormat.encodeToString(ByeMessage())
            ws?.send(text)
        } catch (e: Exception) {
            // 相手の WebSocket が切断済 の場合に Bye を自分が送れなかった場合はログに出すだけ
            Log.w("Ayame", "Failed to send bye: ${e.message}")
        }
    }

    private inline fun <reified T : AyameSignalingMessage> sendJson(msg: T) {
        val text = jsonFormat.encodeToString(msg)
        Log.v("Ayame", "Sending: $text")
        ws?.send(text)
    }

    // 受信メッセージのハンドリング
    private fun handleMessage(text: String) {
        try {
            // typeフィールドだけ先に見て分岐することも可能ですが、
            // ここではpolymorphicなデシリアライズを利用する例、
            // または単純に accept だけ拾う簡易実装で行います。

            val jsonElement = jsonFormat.parseToJsonElement(text)
            when (val type = jsonElement.jsonObject["type"]?.jsonPrimitive?.content) {
                "accept" -> {
                    val msg = jsonFormat.decodeFromString<AcceptMessage>(text)
                    listener?.onAccept(msg)
                }
                "reject" -> {
                    val msg = jsonFormat.decodeFromString<RejectMessage>(text)
                    listener?.onReject(msg)
                }
                "offer" -> {
                    val msg = jsonFormat.decodeFromString<OfferMessage>(text)
                    listener?.onOffer(msg)
                }
                "answer" -> {
                    val msg = jsonFormat.decodeFromString<AnswerMessage>(text)
                    listener?.onAnswer(msg)
                }
                "candidate" -> {
                    val msg = jsonFormat.decodeFromString<CandidateMessage>(text)
                    val iceCandidate = IceCandidate(
                        msg.ice.sdpMid,
                        msg.ice.sdpMLineIndex,
                        msg.ice.candidate
                    )
                    listener?.onCandidate(iceCandidate)
                }
                "ping" -> {
                    sendPong()
                }
                "bye" -> {
                    Log.d("Ayame", "Received bye from server.")
                    disconnect()
                    listener?.onClosed()
                }
                else -> {
                    Log.w("Ayame", "Unknown message type: $type")
                }
            }

        } catch (e: Exception) {
            Log.e("Ayame", "JSON Parse Error: ${e.message}")
        }
    }
}