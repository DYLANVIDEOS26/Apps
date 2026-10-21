package com.example.ui.player

enum class ResizeMode(val label: String) {
    FIT("Ajustar"),
    FILL("Estirar"),
    ZOOM("Zoom")
}

data class PlayerPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLive: Boolean = true,
    val error: String? = null,
    val isMuted: Boolean = false,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val controlsVisible: Boolean = true
)
