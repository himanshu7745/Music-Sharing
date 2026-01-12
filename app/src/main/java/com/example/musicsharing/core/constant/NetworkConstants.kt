package com.example.musicsharing.core.constant

object NetworkConstants {

    // UDP Configuration
    const val DEFAULT_AUDIO_PORT = 49152
    const val UDP_SOCKET_BUFFER_SIZE = 128 * 1024  // 128 KB
    const val MAX_PACKET_SIZE = 4096
    const val UDP_SEND_BUFFER_SIZE = 256 * 1024  // 256 KB

    // Discovery Configuration
    const val BROADCAST_PORT = 8888
    const val BROADCAST_ADDRESS = "255.255.255.255"
    const val BROADCAST_INTERVAL_MS = 2000L
    const val DISCOVERY_DURATION_MS = 10_000L
    const val DISCOVERY_TIMEOUT_MS = 10_500L
    const val DISCOVERY_SOCKET_TIMEOUT_MS = 2000

    // Protocol Configuration
    const val PROTOCOL_PREFIX = "MUSIC_SERVER"
    const val PACKET_MAGIC: Short = 0x4D53  // "MS"
    const val PACKET_VERSION: Byte = 1
    const val PACKET_FLAGS: Byte = 0
    const val PACKET_HEADER_SIZE = 30

    // Network Quality
    const val IPTOS_RELIABILITY = 0x04

    // Thread Configuration
    const val THREAD_JOIN_TIMEOUT_MS = 3000L
    const val SOCKET_TIMEOUT_MS = 1000

    // Connection Quality Thresholds
    const val LATENCY_EXCELLENT = 50L     // < 50ms
    const val LATENCY_GOOD = 150L         // < 150ms
    const val LATENCY_FAIR = 300L         // < 300ms

    const val PACKET_LOSS_EXCELLENT = 0.1f  // < 0.1%
    const val PACKET_LOSS_GOOD = 1f         // < 1%
    const val PACKET_LOSS_FAIR = 5f         // < 5%

    // Signal Strength (RSSI)
    const val RSSI_EXCELLENT = -50  // >= -50 dBm
    const val RSSI_GOOD = -60       // >= -60 dBm
    const val RSSI_FAIR = -70       // >= -70 dBm
    const val RSSI_POOR = -80       // >= -80 dBm
}