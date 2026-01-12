package com.example.musicsharing.core.constant

object AppConstants {

    //App Metadata
    const val APP_NAME = "Music Sharing"
    const val APP_VERSION = "1.0.0"

    //Service Configuration
    const val NOTIFICATION_CHANNEL_ID = "audio_broadcast_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Music Broadcast"
    const val NOTIFICATION_ID = 1

    const val ACTION_STOP_BROADCAST = "STOP_BROADCAST"
    const val EXTRA_AUDIO_URI = "extra_audio_uri"

    const val EXTRA_SERVER_NAME = "extra_server_name"

    // Wake Lock
    const val WAKE_LOCK_TAG = "MusicSharing:ReceiverWakeLock"
    const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L  // 10 minutes

    // Multicast Lock
    const val MULTICAST_LOCK_TAG = "music_receiver_multicast"

    // Server Defaults
    const val DEFAULT_SERVER_NAME = "Unknown Server"
    const val DEFAULT_BITRATE = 192  // kbps
    const val DEFAULT_SAMPLE_RATE = 44100  // Hz
    const val DEFAULT_AUDIO_CODEC = "AAC"
    const val MAX_SERVER_CONNECTIONS = 5

    // Supported Formats
    val SUPPORTED_AUDIO_FORMATS = listOf("MP3", "AAC", "FLAC", "WAV")

    // Clock Sync Configuration
    const val CLOCK_SYNC_ALPHA = 0.1  // Smoothing factor
    const val CLOCK_SYNC_MIN_SAMPLES = 3

}