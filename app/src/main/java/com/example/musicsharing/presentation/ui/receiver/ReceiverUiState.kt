package com.example.musicsharing.presentation.ui.receiver

import com.example.musicsharing.core.constant.NetworkConstants
import com.example.musicsharing.core.model.MusicServerInfo

data class ReceiverUiState(
    // ==================== Core State ====================
    val status: ReceiverStatus = ReceiverStatus.Idle,
    val discoveredServers: List<MusicServerInfo> = emptyList(),
    val selectedServer: MusicServerInfo? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,

    // ==================== Playback Position ====================
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val errorMessage: String? = null,

    // ==================== Network & Connection Info ====================
    val connectionQuality: ConnectionQuality = ConnectionQuality.Unknown,
    val signalStrength: Int = 0, // 0-100
    val latency: Long = 0, // in milliseconds
    val isConnected: Boolean = false,
    val reconnectAttempts: Int = 0,

    // ==================== Audio Info ====================
    val currentTrackTitle: String? = null,
    val currentArtist: String? = null,
    val currentAlbum: String? = null,
    val albumArtUrl: String? = null,
    val bitrate: Int = 0, // in kbps
    val sampleRate: Int = 0, // in Hz
    val audioFormat: String? = null, // e.g., "MP3", "AAC", "FLAC"

    // ==================== Playback Stats ====================
    val buffering: Boolean = false,
    val bufferPercentage: Int = 0,
    val volume: Float = 1.0f, // 0.0 to 1.0
    val isMuted: Boolean = false,

    // ==================== Discovery Stats ====================
    val discoveryProgress: Float = 0f, // 0.0 to 1.0
    val lastDiscoveryTime: Long? = null,
    val totalServersDiscovered: Int = 0,

    // ==================== Session Info ====================
    val sessionStartTime: Long? = null,
    val totalPlayTime: Long = 0, // in milliseconds
    val bytesReceived: Long = 0,
    val packetsReceived: Long = 0,
    val packetsLost: Long = 0,

    // ==================== User Preferences ====================
    val autoReconnect: Boolean = true,
    val showVisualization: Boolean = false,
    val keepScreenOn: Boolean = false,

    // ==================== UI State ====================
    val showServerDetails: Boolean = false,
    val showAudioStats: Boolean = false,
    val expandedServerId: String? = null
)
enum class ConnectionQuality {
    Unknown,
    Excellent,  // < 50ms latency, < 0.1% packet loss
    Good,       // 50-150ms latency, < 1% packet loss
    Fair,       // 150-300ms latency, 1-5% packet loss
    Poor;       // > 300ms latency or > 5% packet loss

    companion object {
        fun fromLatency(latencyMs: Long, packetLossPercent: Float): ConnectionQuality {
            return when {
                latencyMs < NetworkConstants.LATENCY_EXCELLENT &&
                        packetLossPercent < NetworkConstants.PACKET_LOSS_EXCELLENT -> Excellent

                latencyMs < NetworkConstants.LATENCY_GOOD &&
                        packetLossPercent < NetworkConstants.PACKET_LOSS_GOOD -> Good

                latencyMs < NetworkConstants.LATENCY_FAIR &&
                        packetLossPercent < NetworkConstants.PACKET_LOSS_FAIR -> Fair

                else -> Poor
            }
        }
    }
}