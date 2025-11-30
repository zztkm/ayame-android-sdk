package com.velitosoft.sample

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velitosoft.ayame_sdk.AudioController
import com.velitosoft.ayame_sdk.AyameListener
import com.velitosoft.ayame_sdk.AyameMediaChannel
import com.velitosoft.ayame_sdk.AyameOptions
import com.velitosoft.ayame_sdk.VideoController
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.MediaStream

private const val TAG = "VideoChatViewModel"

/**
 * Connection state for the video chat.
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * UI state for the video chat screen.
 */
data class VideoChatState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val localStream: MediaStream? = null,
    val remoteStream: MediaStream? = null,
    val errorMessage: String? = null,
    val permissionsGranted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val isCapturing: Boolean = false
)

/**
 * ViewModel for managing video chat state and Ayame SDK integration.
 */
class VideoChatViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(VideoChatState())
        private set

    private var ayameChannel: AyameMediaChannel? = null

    val eglBaseContext: EglBase.Context?
        get() = ayameChannel?.eglBaseContext

    val videoController: VideoController?
        get() = ayameChannel?.videoController

    val audioController: AudioController?
        get() = ayameChannel?.audioController

    /**
     * Initialize Ayame SDK when permissions are granted.
     */
    fun setPermissionsGranted(granted: Boolean) {
        state = state.copy(permissionsGranted = granted)

        if (granted && ayameChannel == null) {
            initializeAyameChannel()
        }
    }

    /**
     * Initialize the Ayame media channel with configuration from BuildConfig.
     */
    private fun initializeAyameChannel() {
        try {
            Log.d(TAG, "Initializing Ayame channel with:")
            Log.d(TAG, "  Signaling URL: ${BuildConfig.AYAME_SIGNALING_URL}")
            Log.d(TAG, "  Room ID: ${BuildConfig.AYAME_ROOM_ID}")

            ayameChannel = AyameMediaChannel(
                signalingUrl = BuildConfig.AYAME_SIGNALING_URL,
                roomId = BuildConfig.AYAME_ROOM_ID,
                context = getApplication(),
                options = AyameOptions(
                    audioEnabled = true,
                    videoEnabled = true
                ),
                signalingKey = BuildConfig.AYAME_SIGNALING_KEY.takeIf { it.isNotBlank() }
            ).apply {
                listener = createAyameListener()
            }

            Log.d(TAG, "Ayame channel initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Ayame channel", e)
            state = state.copy(
                connectionState = ConnectionState.ERROR,
                errorMessage = "Failed to initialize: ${e.message}"
            )
        }
    }

    /**
     * Create AyameListener to handle SDK callbacks.
     */
    private fun createAyameListener() = object : AyameListener {
        override fun onConnected() {
            Log.d(TAG, "Connected to peer")
            viewModelScope.launch {
                state = state.copy(
                    connectionState = ConnectionState.CONNECTED,
                    errorMessage = null,
                    isCapturing = videoController?.isCapturing ?: false
                )
            }
        }

        override fun onDisconnected(reason: String) {
            Log.d(TAG, "Disconnected: $reason")
            viewModelScope.launch {
                state = state.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    localStream = null,
                    remoteStream = null,
                    errorMessage = if (reason.isNotBlank()) "Disconnected: $reason" else null,
                    isCapturing = false
                )
            }
        }

        override fun onError(error: String) {
            Log.e(TAG, "Error: $error")
            viewModelScope.launch {
                state = state.copy(
                    connectionState = ConnectionState.ERROR,
                    errorMessage = error
                )
            }
        }

        override fun onAddRemoteStream(stream: MediaStream) {
            Log.d(TAG, "Stream added: ${stream.id}")
            viewModelScope.launch {
                // First stream is local, second is remote
                if (state.localStream == null) {
                    Log.d(TAG, "Setting local stream: ${stream.id}")
                    state = state.copy(localStream = stream)
                } else if (state.remoteStream == null) {
                    Log.d(TAG, "Setting remote stream: ${stream.id}")
                    state = state.copy(remoteStream = stream)
                } else {
                    Log.w(TAG, "Received unexpected stream: ${stream.id}")
                }
            }
        }

        override fun onRemoveRemoteStream(stream: MediaStream) {
            Log.d(TAG, "Stream removed: ${stream.id}")
            viewModelScope.launch {
                when (stream.id) {
                    state.localStream?.id -> {
                        Log.d(TAG, "Removing local stream")
                        state = state.copy(localStream = null)
                    }
                    state.remoteStream?.id -> {
                        Log.d(TAG, "Removing remote stream")
                        state = state.copy(remoteStream = null)
                    }
                }
            }
        }
    }

    /**
     * Start connection to Ayame signaling server.
     */
    fun connect() {
        if (!state.permissionsGranted) {
            Log.w(TAG, "Cannot connect: permissions not granted")
            state = state.copy(
                errorMessage = "Camera and microphone permissions required"
            )
            return
        }

        if (ayameChannel == null) {
            initializeAyameChannel()
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting connection...")
                state = state.copy(
                    connectionState = ConnectionState.CONNECTING,
                    errorMessage = null
                )

                ayameChannel?.connect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                state = state.copy(
                    connectionState = ConnectionState.ERROR,
                    errorMessage = "Failed to connect: ${e.message}"
                )
            }
        }
    }

    /**
     * Disconnect from the video chat.
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Disconnecting...")
                ayameChannel?.disconnect()
                state = state.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    localStream = null,
                    remoteStream = null,
                    errorMessage = null,
                    isCapturing = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                state = state.copy(
                    errorMessage = "Failed to disconnect: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle video track on/off.
     */
    fun toggleVideo() {
        videoController?.let { controller ->
            controller.isEnabled = !controller.isEnabled
            state = state.copy(isVideoEnabled = controller.isEnabled)
            Log.d(TAG, "Video ${if (controller.isEnabled) "enabled" else "disabled"}")
        }
    }

    /**
     * Toggle audio track on/off.
     */
    fun toggleAudio() {
        audioController?.let { controller ->
            controller.isEnabled = !controller.isEnabled
            state = state.copy(isAudioEnabled = controller.isEnabled)
            Log.d(TAG, "Audio ${if (controller.isEnabled) "enabled" else "disabled"}")
        }
    }

    /**
     * Switch between front and back camera.
     */
    fun switchCamera() {
        videoController?.switchCamera { isFront ->
            Log.d(TAG, "Switched to ${if (isFront) "front" else "back"} camera")
        }
    }

    /**
     * Clean up resources when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, disconnecting...")
        ayameChannel?.disconnect()
        ayameChannel = null
    }
}
