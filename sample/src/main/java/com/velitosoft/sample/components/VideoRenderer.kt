package com.velitosoft.sample.components

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * Composable that renders WebRTC video stream using SurfaceViewRenderer.
 *
 * @param eglBaseContext EGL context for rendering (from AyameMediaChannel)
 * @param mediaStream MediaStream containing video track to render
 * @param mirror Whether to mirror the video (true for front camera)
 * @param modifier Modifier for the composable
 */
@Composable
fun VideoRenderer(
    eglBaseContext: EglBase.Context,
    mediaStream: MediaStream?,
    mirror: Boolean = false,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Initialize the renderer with EGL context
                init(eglBaseContext, null)
                // Set mirroring for front camera
                setMirror(mirror)
                // Scale video to fit while maintaining aspect ratio
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                // Enable hardware scaler for better performance
                setEnableHardwareScaler(true)
                // Set background to black
                setZOrderMediaOverlay(false)
            }
        },
        update = { view ->
            // Update video track when mediaStream changes
            mediaStream?.videoTracks?.firstOrNull()?.let { videoTrack ->
                videoTrack.addSink(view)
            } ?: run {
                // Clear the renderer if no stream
                view.clearImage()
            }
        },
        onRelease = { view ->
            // Clean up when the view is released
            mediaStream?.videoTracks?.firstOrNull()?.removeSink(view)
            view.release()
        },
        modifier = modifier
    )
}
