package com.example.musicsharing.domain.audio.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.example.musicsharing.core.constant.AudioConstants
import java.util.concurrent.atomic.AtomicBoolean

class AudioTrackPlayer {

    companion object {
        private const val TAG = "AudioTrackPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)

    fun prepare() {
        if (audioTrack != null) return

        val bufferDurationMs = AudioConstants.AUDIO_TRACK_BUFFER_MS
        val bytesPerMs = AudioConstants.SAMPLE_RATE * AudioConstants.BYTES_PER_SAMPLE / 1000
        val targetBufferSize = bufferDurationMs * bytesPerMs

        val minBufferSize = AudioTrack.getMinBufferSize(
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL_CONFIG,
            AudioConstants.AUDIO_ENCODING
        )

        val finalBufferSize = maxOf(minBufferSize, targetBufferSize)

        // Use low-latency audio path
        val attributesBuilder = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

        // Request low latency if available (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attributesBuilder.setFlags(AudioAttributes.FLAG_LOW_LATENCY)
        }

        val formatBuilder = AudioFormat.Builder()
            .setSampleRate(AudioConstants.SAMPLE_RATE)
            .setEncoding(AudioConstants.AUDIO_ENCODING)
            .setChannelMask(AudioConstants.CHANNEL_CONFIG)

        audioTrack = AudioTrack(
            attributesBuilder.build(),
            formatBuilder.build(),
            finalBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        val actualFrames = audioTrack?.bufferSizeInFrames ?: 0
        val actualLatencyMs = actualFrames * 1000 / AudioConstants.SAMPLE_RATE

        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "AudioTrack prepared:")
        Log.i(TAG, "  Buffer: $finalBufferSize bytes ($bufferDurationMs ms target)")
        Log.i(TAG, "  Actual: $actualFrames frames (~${actualLatencyMs}ms)")
        Log.i(TAG, "  Min required: $minBufferSize bytes")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "  Low-latency mode: ENABLED")
        }

        if (actualLatencyMs > 500) {
            Log.w(TAG, "  ⚠ WARNING: Large AudioTrack buffer may add latency!")
        }
        Log.i(TAG, "═══════════════════════════════════════════════════════")
    }

    fun play() {
        val track = audioTrack ?: throw IllegalStateException("AudioTrack not prepared")
        if (isPlaying.compareAndSet(false, true)) {
            track.play()
            Log.d(TAG, "AudioTrack started")
        }
    }

    fun write(pcmData: ByteArray) {
        val track = audioTrack ?: return
        if (!isPlaying.get()) return

        var offset = 0
        while (offset < pcmData.size) {
            val written = track.write(
                pcmData,
                offset,
                pcmData.size - offset,
                AudioTrack.WRITE_BLOCKING
            )
            if (written <= 0) {
                Log.w(TAG, "AudioTrack write failed: $written")
                break
            }
            offset += written
        }
    }

    fun release() {
        isPlaying.set(false)
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
        Log.d(TAG, "AudioTrack released")
    }

}