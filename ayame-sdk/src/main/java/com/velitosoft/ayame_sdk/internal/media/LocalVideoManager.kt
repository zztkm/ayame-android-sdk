package com.velitosoft.ayame_sdk.internal.media

import android.content.Context
import android.util.Log
import com.velitosoft.ayame_sdk.AyameOptions
import com.velitosoft.ayame_sdk.VideoController
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID
import kotlin.uuid.Uuid

class LocalVideoManager(
    private val context: Context,
    private val factory: PeerConnectionFactory,
    private val rootEglBaseContext: EglBase.Context,
    private val options: AyameOptions,
    private val customCapturer: VideoCapturer? = null,
): VideoController {
    private var capturer: VideoCapturer? = null
    var track: VideoTrack? = null
        private set

    // TODO(zztkm): AyameOptions から設定できるようにする
    private var captureWidth = 640
    private var captureHeight = 480
    private var captureFps = 30

    private var _isCapturing = false

    override var isEnabled: Boolean
        get() = track?.enabled() ?: false
        set(value) {
            try {
                // トラックがなければ無視される
                track?.setEnabled(value)
            } catch (e: Exception) {
                Log.w("LocalVideoManager", "Failed to set video track enabled: $e")
            }
        }

    override val isCapturing: Boolean
        get() = _isCapturing

    override fun startCapture(): Boolean {
        // すでにキャプチャ中の場合は成功扱いとする
        if (_isCapturing) return true

        val capturer = capturer ?: run {
            Log.w("LocalVideoManager", "Camera capturer is not initialized")
            return false
        }

        return try {
            capturer.startCapture(captureWidth, captureHeight, captureFps)
            _isCapturing = true
            true
        } catch (e: Exception) {
            Log.e("LocalVideoManager", "Failed to start video capture: $e")
            false
        }
    }

    override fun stopCapture() {
        if (!_isCapturing) {
            Log.w("LocalVideoManager", "Video capture is not started")
            return
        }

        val capturer = capturer ?: run {
            Log.w("LocalVideoManager", "Camera capturer is not initialized")
            return
        }

        try {
            capturer.stopCapture()
        } catch (e: Exception) {
            Log.e("LocalVideoManager", "Failed to stop video capture: $e")
        } finally {
            _isCapturing = false
        }
    }

    override fun switchCamera(callback: ((isFront: Boolean) -> Unit)?) {
        if (!_isCapturing) {
            Log.w("LocalVideoManager", "Cannot switch camera when not capturing")
            return
        }

        // CameraVideoCapturer でなければ切り替え不可
        val capturer = capturer as? CameraVideoCapturer ?: run {
            Log.w("LocalVideoManager", "Camera switching is not supported for this capturer")
            return
        }

        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                callback?.invoke(isFrontCamera)
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.e("LocalVideoManager", "Failed to switch camera: $errorDescription")
            }
        })
    }

    fun start() {
        if (!options.videoEnabled) return

        capturer = customCapturer ?: createCameraCapturer(Camera2Enumerator(context)) ?: run {
            Log.e("LocalVideoManager", "Failed to create camera capturer")
            return
        }

        val isScreencast = capturer?.isScreencast ?: false
        val source = factory.createVideoSource(isScreencast)

        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBaseContext)
        capturer?.initialize(surfaceTextureHelper, context, source.capturerObserver)

        track = factory.createVideoTrack(UUID.randomUUID().toString(), source)

        startCapture()
    }

    fun stop() {
        stopCapture()
        capturer?.dispose()
        track?.dispose()

        capturer = null
        track = null
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): CameraVideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // 前面カメラを優先して探す
        deviceNames.firstOrNull { enumerator.isFrontFacing(it) }?.let {
            return enumerator.createCapturer(it, null)
        }

        // 背面カメラを探す
        deviceNames.firstOrNull { enumerator.isBackFacing(it) }?.let {
            return enumerator.createCapturer(it, null)
        }

        // カメラが見つからなかった
        return null
    }
}
