package com.example.musicsharing.core.constant

import android.media.AudioFormat

object AudioConstants {

    // Audio Format
    const val SAMPLE_RATE = 44100
    const val CHANNEL_COUNT = 1
    const val BYTES_PER_SAMPLE = 2

    // AudioTrack Configuration
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

    // Frame Configuration
    const val FRAME_INTERVAL_MS = 20L
    const val SAMPLES_PER_FRAME = SAMPLE_RATE * FRAME_INTERVAL_MS / 1000
    const val FRAME_BYTES = SAMPLES_PER_FRAME * CHANNEL_COUNT * BYTES_PER_SAMPLE

    // Timing Configuration
    const val BUFFER_DELAY_MS = 100L  // Initial buffering delay

    // Jitter Buffer Configuration
    const val LATE_TOLERANCE_MS = 100L
    const val EARLY_TOLERANCE_MS = 20L
    const val MAX_JITTER_BUFFER_MS = 500L
    const val MIN_BUFFER_FRAMES = 3
    const val TARGET_BUFFER_FRAMES = 5

    // AudioTrack Configuration
    const val AUDIO_TRACK_BUFFER_MS = 200  // Hardware buffer size in ms

    const val CODEC_DEQUEUE_TIMEOUT_US = FRAME_INTERVAL_MS * 1000


    // Packet Expiration
    const val PACKET_EXPIRE_MS = 200L

}