package com.velitosoft.ayame_sdk

interface VideoController {
    /**
     * 映像トラックの有効 / 無効状態。
     * true: 送出中 / false: 黒画面を送出
     * カメラデバイスは停止しない。
     */
    var isEnabled: Boolean

    /**
     * 映像キャプチャ中かどうか。
     *
     * false の場合、カメラのインジケーターが点灯しない。
     */
    val isCapturing: Boolean

    /**
     * 映像キャプチャを開始する。
     *
     * @return 開始に成功した場合 true、失敗または既に開始中の場合 false
     */
    fun startCapture(): Boolean

    /**
     * 映像キャプチャを停止する。
     */
    fun stopCapture()

    /**
     * カメラの前面 / 背面を切り替える。
     *
     * isCapturing = false のときは呼び出せない。
     */
    fun switchCamera(callback: ((isFront: Boolean) -> Unit)? = null)
}

interface AudioController {
    /**
     * 音声トラックの有効 / 無効状態。
     * true: 送出中 / false: ミュート
     *
     * マイクデバイスは停止しない。
     */
    var isEnabled: Boolean

    /**
     * 音声録音を開始する。
     *
     * @return 録音の開始に成功した場合 true、失敗または既に開始中の場合 false
     */
    fun startRecording(): Boolean

    /**
     * 音声録音を停止する。
     */
    fun stopRecording()
}