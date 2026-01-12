package com.example.musicsharing.domain.network.receiver

import android.util.Log
import com.example.musicsharing.core.model.BufferedFrame
import com.example.musicsharing.domain.audio.packet.AudioPacketParser
import com.example.musicsharing.domain.audio.player.AudioTrackPlayer
import com.example.musicsharing.core.constant.AudioConstants
import java.util.concurrent.atomic.AtomicBoolean

class ReceiverEngine(
    private val listenPort: Int
) {

    companion object {
        private const val TAG = "ReceiverEngine"
        private val silenceFrame = ByteArray(AudioConstants.FRAME_BYTES.toInt())
    }

    private val running = AtomicBoolean(false)

    private val clock = ClockOffsetEstimator()
    private val jitterBuffer = JitterBuffer()
    private val audioPlayer = AudioTrackPlayer()

    private lateinit var udpReceiver: UdpAudioReceiver
    private var audioThread: Thread? = null

    private var silenceFramesPlayed = 0
    private var audioFramesPlayed = 0
    private var packetsProcessed = 0
    private var packetsInvalid = 0
    private var packetsExpired = 0
    private var startTimeMs = 0L

    fun start() {
        if (!running.compareAndSet(false, true)) return

        startTimeMs = System.currentTimeMillis()

        audioPlayer.prepare()
        audioPlayer.play()

        udpReceiver = UdpAudioReceiver(listenPort) { data, length ->
            handlePacket(data, length)
        }
        udpReceiver.start()

        startAudioLoop()

        Log.d(TAG, "ReceiverEngine started on port $listenPort")
    }

    fun stop() {
        running.set(false)
        udpReceiver.stop()

        try {
            audioThread?.interrupt()
            audioThread?.join(2000)
        } catch (_: InterruptedException) {
        }

        audioPlayer.release()

        val totalFrames = audioFramesPlayed + silenceFramesPlayed
        val silencePercent = if (totalFrames > 0) {
            silenceFramesPlayed * 100.0 / totalFrames
        } else 0.0

        val durationSec = (System.currentTimeMillis() - startTimeMs) / 1000.0
        val jitterStats = jitterBuffer.getStats()

        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "ReceiverEngine STOPPED (ran ${String.format("%.1f", durationSec)}s)")
        Log.i(TAG, "───────────────────────────────────────────────────────")
        Log.i(TAG, "Packets: processed=$packetsProcessed, invalid=$packetsInvalid, expired=$packetsExpired")
        Log.i(TAG, "Audio: frames=$audioFramesPlayed, silence=$silenceFramesPlayed (${String.format("%.1f", silencePercent)}%)")
        Log.i(TAG, "Buffer: $jitterStats")
        Log.i(TAG, "Latency: ~${AudioConstants.BUFFER_DELAY_MS + AudioConstants.MIN_BUFFER_FRAMES * AudioConstants.FRAME_INTERVAL_MS}ms target")
        Log.i(TAG, "═══════════════════════════════════════════════════════")

        if (silencePercent > 15) {
            Log.w(TAG, "⚠ HIGH SILENCE - Increase BUFFER_DELAY_MS or MIN_BUFFER_FRAMES")
        } else if (silencePercent < 5) {
            Log.i(TAG, "✓ EXCELLENT QUALITY")
        } else {
            Log.i(TAG, "✓ GOOD QUALITY")
        }

        jitterBuffer.clear()
    }

    private fun handlePacket(data: ByteArray, length: Int) {
        val packet = AudioPacketParser.parse(data, length)

        if (packet == null) {
            packetsInvalid++
            return
        }

        packetsProcessed++

        val nowLocal = System.currentTimeMillis()
        clock.update(packet.playAtMs, nowLocal)

        val nowSenderTime = clock.toSenderTime(nowLocal)

        // Check if packet is expired
        if (nowSenderTime > packet.expireAtMs) {
            packetsExpired++
            return
        }

        jitterBuffer.offer(
            frame = BufferedFrame(
                playAtMs = packet.playAtMs,
                pcmData = packet.pcmData
            ),
            nowSenderTime = nowSenderTime
        )
    }

    private fun startAudioLoop() {
        audioThread = Thread {
            var lastBufferCheck = System.currentTimeMillis()
            var consecutiveSilence = 0

            while (running.get()) {
                val nowSenderTime = clock.toSenderTime(System.currentTimeMillis())
                val frames = jitterBuffer.pollReadyFrames(nowSenderTime)

                if (frames.isNotEmpty()) {
                    // Play all ready frames
                    for (frame in frames) {
                        audioPlayer.write(frame.pcmData)
                        audioFramesPlayed++
                    }
                    consecutiveSilence = 0
                } else {
                    // If buffering, don't inject silence - just wait
                    if (!jitterBuffer.isBuffering()) {
                        audioPlayer.write(silenceFrame)
                        silenceFramesPlayed++
                        consecutiveSilence++

                        if (consecutiveSilence == 50) {  // 1 second of silence
                            Log.w(TAG, "⚠ 1s of silence - buffer may be too small")
                        }
                    }
                }

                // Periodic buffer health check
                val now = System.currentTimeMillis()
                if (now - lastBufferCheck > 10000) {  // Every 10 seconds
                    val bufferSize = jitterBuffer.size()
                    val target = AudioConstants.TARGET_BUFFER_FRAMES
                    val avgBuffer = if (audioFramesPlayed > 0) bufferSize else 0
                    Log.d(TAG, "Buffer: $bufferSize frames, silence: ${String.format("%.1f", silenceFramesPlayed * 100.0 / (audioFramesPlayed + silenceFramesPlayed))}%")
                    lastBufferCheck = now
                }

                try {
                    Thread.sleep(AudioConstants.FRAME_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.apply {
            name = "ReceiverAudioThread"
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }
    }
}