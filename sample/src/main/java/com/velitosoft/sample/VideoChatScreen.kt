package com.velitosoft.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.velitosoft.sample.components.ControlPanel
import com.velitosoft.sample.components.VideoRenderer
import org.webrtc.EglBase

/**
 * Main video chat screen with local and remote video feeds and controls.
 *
 * @param state Current video chat state
 * @param eglBaseContext EGL context for video rendering
 * @param onConnect Callback when connect button is clicked
 * @param onDisconnect Callback when disconnect button is clicked
 * @param onToggleVideo Callback when video toggle is clicked
 * @param onToggleAudio Callback when audio toggle is clicked
 * @param onSwitchCamera Callback when camera switch is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun VideoChatScreen(
    state: VideoChatState,
    eglBaseContext: EglBase.Context?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages in snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background: Remote video or placeholder
        if (state.remoteStream != null && eglBaseContext != null) {
            VideoRenderer(
                eglBaseContext = eglBaseContext,
                mediaStream = state.remoteStream,
                mirror = false,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder background when no remote stream
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (state.connectionState) {
                        ConnectionState.DISCONNECTED -> stringResource(R.string.waiting_to_connect)
                        ConnectionState.CONNECTING -> stringResource(R.string.connecting)
                        ConnectionState.CONNECTED -> stringResource(R.string.waiting_for_peer)
                        ConnectionState.ERROR -> stringResource(R.string.connection_error)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }

        // Foreground: Local video (picture-in-picture)
        if (state.localStream != null && eglBaseContext != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                VideoRenderer(
                    eglBaseContext = eglBaseContext,
                    mediaStream = state.localStream,
                    mirror = true, // Mirror front camera
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Loading indicator when connecting
        if (state.connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Control panel at bottom
        ControlPanel(
            isConnected = state.connectionState == ConnectionState.CONNECTED,
            isVideoEnabled = state.isVideoEnabled,
            isAudioEnabled = state.isAudioEnabled,
            isCapturing = state.isCapturing,
            onConnectClick = onConnect,
            onDisconnectClick = onDisconnect,
            onToggleVideo = onToggleVideo,
            onToggleAudio = onToggleAudio,
            onSwitchCamera = onSwitchCamera,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Snackbar for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
