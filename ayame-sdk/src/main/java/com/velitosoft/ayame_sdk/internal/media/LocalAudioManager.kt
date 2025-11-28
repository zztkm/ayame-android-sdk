package com.velitosoft.ayame_sdk.internal.media

import android.util.Log
import com.velitosoft.ayame_sdk.AudioController
import com.velitosoft.ayame_sdk.AyameOptions
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory

internal class LocalAudioManager(
    private val factory: PeerConnectionFactory,
    private val options: AyameOptions
) : AudioController {

    private var audioSource: AudioSource? = null
    var track: AudioTrack? = null
        private set

    override var isEnabled: Boolean
        get() = track?.enabled() ?: false
        set(value) {
            try {
                track?.setEnabled(value)
            } catch (e: Exception) {
                Log.w("AyameAudio", "Failed to set audio track enabled: ${e.message}")
            }
        }

    override fun startRecording(): Boolean {
        // TODO(zztkm): 録音開始を行うようにする
        return if (track != null) {
            isEnabled = true
            true
        } else {
            false
        }
    }

    override fun stopRecording() {
        // TODO(zztkm): 録音停止を行うようにする
        isEnabled = false
    }

    fun start() {
        if (!options.audioEnabled) return

        val constraints = MediaConstraints()
        // エコーキャンセル等の設定が必要ならここで追加

        audioSource = factory.createAudioSource(constraints)
        track = factory.createAudioTrack("ARDAMSa0", audioSource)
        track?.setEnabled(true)
    }

    fun stop() {
        audioSource?.dispose()
        track?.dispose()
        audioSource = null
        track = null
    }
}