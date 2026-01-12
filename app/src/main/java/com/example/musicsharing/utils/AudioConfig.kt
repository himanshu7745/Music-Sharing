package com.example.musicsharing.utils

import android.media.AudioFormat

object AudioConfig {

    // Timing Configuration - Optimized for low latency
    const val FRAME_INTERVAL_MS = 20L
    const val BUFFER_DELAY_MS = 100L  // Reduced from 250ms - just enough to start

    // Audio Format
    const val SAMPLE_RATE = 44100
    const val CHANNEL_COUNT = 1
    const val BYTES_PER_SAMPLE = 2

    // Frame Configuration
    const val SAMPLES_PER_FRAME =
        SAMPLE_RATE * FRAME_INTERVAL_MS / 1000

    const val FRAME_BYTES =
        SAMPLES_PER_FRAME * CHANNEL_COUNT * BYTES_PER_SAMPLE

    // Jitter Buffer Configuration - Minimal buffering
    const val LATE_TOLERANCE_MS = 100L  // Reduced but still reasonable
    const val EARLY_TOLERANCE_MS = 20L
    const val MAX_JITTER_BUFFER_MS = 500L  // Reduced max buffer
    const val MIN_BUFFER_FRAMES = 3  // Start quickly with fewer frames
    const val TARGET_BUFFER_FRAMES = 5  // Lower target

    // AudioTrack Configuration - Smaller hardware buffer
    const val AUDIO_TRACK_BUFFER_MS = 200  // Reduced from 400ms

    // AudioTrack Metadata
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

    // Network Configuration
    const val UDP_SOCKET_BUFFER_SIZE = 128 * 1024  // Smaller buffer, less queuing
    const val MAX_PACKET_SIZE = 4096
    const val PACKET_EXPIRE_MS = 200L  // Shorter expiration
}