package com.velitosoft.ayame_sdk

data class AyameOptions(
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val videoCodec: VideoCodec = VideoCodec.VP9,
) {
    companion object {
        fun default(): AyameOptions {
            return AyameOptions()
        }
    }
}

enum class VideoCodec {
    VP8,
    VP9,
    H264,
    H265,
    AV1,
}
