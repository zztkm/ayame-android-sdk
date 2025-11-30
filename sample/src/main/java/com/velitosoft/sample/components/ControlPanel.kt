package com.velitosoft.sample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.velitosoft.sample.R

/**
 * Control panel for video chat with connect/disconnect and media controls.
 *
 * @param isConnected Whether the video chat is currently connected
 * @param isVideoEnabled Whether video is enabled
 * @param isAudioEnabled Whether audio is enabled
 * @param isCapturing Whether camera is actively capturing
 * @param onConnectClick Callback when connect button is clicked
 * @param onDisconnectClick Callback when disconnect button is clicked
 * @param onToggleVideo Callback when video toggle is clicked
 * @param onToggleAudio Callback when audio toggle is clicked
 * @param onSwitchCamera Callback when camera switch is clicked
 * @param modifier Modifier for the composable
 */
@Composable
fun ControlPanel(
    isConnected: Boolean,
    isVideoEnabled: Boolean,
    isAudioEnabled: Boolean,
    isCapturing: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connect/Disconnect Button
        FilledTonalButton(
            onClick = if (isConnected) onDisconnectClick else onConnectClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CallEnd else Icons.Default.Call,
                contentDescription = if (isConnected)
                    stringResource(R.string.disconnect)
                else
                    stringResource(R.string.connect)
            )
            Text(
                text = if (isConnected)
                    stringResource(R.string.disconnect)
                else
                    stringResource(R.string.connect),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Video Toggle Button
        IconButton(
            onClick = onToggleVideo,
            enabled = isConnected
        ) {
            Icon(
                imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = stringResource(R.string.toggle_video),
                tint = if (isConnected) {
                    if (isVideoEnabled) Color.White else MaterialTheme.colorScheme.error
                } else {
                    Color.Gray
                }
            )
        }

        // Audio Toggle Button
        IconButton(
            onClick = onToggleAudio,
            enabled = isConnected
        ) {
            Icon(
                imageVector = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = stringResource(R.string.toggle_audio),
                tint = if (isConnected) {
                    if (isAudioEnabled) Color.White else MaterialTheme.colorScheme.error
                } else {
                    Color.Gray
                }
            )
        }

        // Camera Switch Button
        IconButton(
            onClick = onSwitchCamera,
            enabled = isConnected && isCapturing
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = stringResource(R.string.switch_camera),
                tint = if (isConnected && isCapturing) Color.White else Color.Gray
            )
        }
    }
}
