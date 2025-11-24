package com.velitosoft.ayame_sdk

import org.webrtc.MediaStream

interface AyameListener {
    fun onConnected()
    fun onDisconnected(reason: String)
    fun onError(error: String)

    fun onAddRemoteStream(stream: MediaStream)
    fun onRemoveRemoteStream(stream: MediaStream)
}