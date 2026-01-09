package com.example.musicsharing.ui.receiver

import com.example.musicsharing.utils.MusicServerInfo

data class ReceiverUiState(
    val status: ReceiverStatus = ReceiverStatus.Idle,
    val discoveredServers: List<MusicServerInfo> = emptyList(),
    val selectedServer: MusicServerInfo? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val errorMessage: String? = null
)