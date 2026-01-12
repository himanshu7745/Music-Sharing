package com.example.musicsharing.presentation.ui.sender

import android.net.Uri

data class SenderUiState(
    val selectedAudioUri: Uri? = null,
    val isPlaying: Boolean = false,
    val isBroadcasting: Boolean = false,
    val broadcastIpAddress: String? = null,
    val broadcastPort: Int? = null,
    val serverName: String = "",
    val statusMessage: String = "No audio file selected"
)