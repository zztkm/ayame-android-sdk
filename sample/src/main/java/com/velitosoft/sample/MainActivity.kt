package com.velitosoft.sample

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velitosoft.sample.ui.theme.AyameSDKTheme

/**
 * Main activity for the Ayame SDK video chat sample app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AyameSDKTheme {
                VideoChatApp()
            }
        }
    }
}

/**
 * Main app composable with permission handling and video chat screen.
 */
@Composable
fun VideoChatApp() {
    val viewModel: VideoChatViewModel = viewModel()
    var permissionsGranted by remember { mutableStateOf(false) }

    // Permission launcher for camera and microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val granted = cameraGranted && audioGranted

        permissionsGranted = granted
        viewModel.setPermissionsGranted(granted)
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        VideoChatScreen(
            state = viewModel.state,
            eglBaseContext = viewModel.eglBaseContext,
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onToggleVideo = viewModel::toggleVideo,
            onToggleAudio = viewModel::toggleAudio,
            onSwitchCamera = viewModel::switchCamera,
            modifier = Modifier.padding(innerPadding)
        )
    }
}