package com.example.musicsharing.core.model

import com.example.musicsharing.core.constant.AppConstants.DEFAULT_AUDIO_CODEC
import com.example.musicsharing.core.constant.AppConstants.DEFAULT_BITRATE
import com.example.musicsharing.core.constant.AppConstants.DEFAULT_SAMPLE_RATE
import com.example.musicsharing.core.constant.AppConstants.DEFAULT_SERVER_NAME
import com.example.musicsharing.core.constant.AppConstants.MAX_SERVER_CONNECTIONS
import com.example.musicsharing.core.constant.AppConstants.SUPPORTED_AUDIO_FORMATS
import com.example.musicsharing.core.constant.NetworkConstants.RSSI_EXCELLENT
import com.example.musicsharing.core.constant.NetworkConstants.RSSI_FAIR
import com.example.musicsharing.core.constant.NetworkConstants.RSSI_GOOD
import com.example.musicsharing.core.constant.NetworkConstants.RSSI_POOR
import java.util.UUID

data class MusicServerInfo(
    // Server Identity
    val id: String = UUID.randomUUID().toString(),
    val serverName: String = DEFAULT_SERVER_NAME,

    // Network Info
    val ipAddress: String,
    val port: Int,

    // Server Capabilities

    val supportedFormats: List<String> = SUPPORTED_AUDIO_FORMATS,
    val maxBitrate: Int = 320, // kbps
    val maxConnections: Int = MAX_SERVER_CONNECTIONS,
    val currentConnections: Int = 0,

    // Audio Quality
    val defaultBitrate: Int = DEFAULT_BITRATE, // kbps
    val defaultSampleRate: Int = DEFAULT_SAMPLE_RATE, // Hz
    val audioCodec: String = DEFAULT_AUDIO_CODEC,

    // Discovery Info
    val discoveredAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val rssi: Int = 0, // WiFi signal strength in dBm
    val signalQuality: SignalQuality = SignalQuality.fromRssi(rssi),

    // Server Status
    val isOnline: Boolean = true,
    val isStreaming: Boolean = false,
    val serverLoad: Float = 0f, // 0.0 to 1.0

    // Security & Auth
    val requiresAuth: Boolean = false,
    val isSecure: Boolean = false,
    val certificateValid: Boolean = true,

    // Additional Metadata
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val customData: Map<String, String> = emptyMap()
) {

    val hasCapacity: Boolean
        get() = currentConnections < maxConnections

    val displayName: String?
        get() = serverName.takeIf { it != "Unknown Server" }

    val connectionUrl: String
        get() = "$ipAddress:$port"

    // Helper Methods
    fun getQualityRating(): ServerQuality {
        return when {
            !isOnline -> ServerQuality.Offline
            serverLoad > 0.9f -> ServerQuality.Overloaded
            !hasCapacity -> ServerQuality.Full
            signalQuality == SignalQuality.Excellent && maxBitrate >= 320 -> ServerQuality.Excellent
            signalQuality >= SignalQuality.Good && maxBitrate >= 192 -> ServerQuality.Good
            signalQuality >= SignalQuality.Fair -> ServerQuality.Fair
            else -> ServerQuality.Poor
        }
    }

}

enum class SignalQuality(val displayName: String) {
    Excellent("Excellent"),
    Good("Good"),
    Fair("Fair"),
    Poor("Poor"),
    VeryPoor("Very Poor");

    companion object {
        fun fromRssi(rssi: Int): SignalQuality {
            return when {
                rssi >= RSSI_EXCELLENT -> Excellent
                rssi >= RSSI_GOOD -> Good
                rssi >= RSSI_FAIR -> Fair
                rssi >= RSSI_POOR -> Poor
                else -> VeryPoor
            }
        }

    }
}

enum class ServerQuality {
    Excellent,
    Good,
    Fair,
    Poor,
    Full,
    Overloaded,
    Offline
}

