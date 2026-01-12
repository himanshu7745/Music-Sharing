package com.example.musicsharing.domain.network.sender

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.musicsharing.core.constant.AudioConstants
import com.example.musicsharing.core.model.AudioPacket
import com.example.musicsharing.domain.audio.decoder.AudioDecoder

class SenderEngine(
    private val context: Context,
    private val audioUri: Uri,
    private val targetIp: String,
    private val port: Int
) {

    companion object {
        private const val TAG = "SenderEngine"
    }

    @Volatile
    private var running = false

    private var workerThread: Thread? = null
    private var udpSender: UdpAudioSender? = null

    fun start() {
        if (running) return

        running = true

        workerThread = Thread {
            runSenderLoop()
        }.apply {
            name = "AudioSenderThread"
            priority = Thread.NORM_PRIORITY + 1
            isDaemon = true
            start()
        }

        Log.d(TAG, "SenderEngine started, streaming to $targetIp:$port")
    }

    fun stop() {
        running = false

        try {
            workerThread?.interrupt()
            workerThread?.join(2000)
        } catch (_: InterruptedException) {
        }

        workerThread = null

        udpSender?.close()
        udpSender = null

        Log.d(TAG, "SenderEngine stopped")
    }

    private fun runSenderLoop() {

        val startTime = System.currentTimeMillis() + AudioConstants.BUFFER_DELAY_MS
        val decoder = AudioDecoder(context, audioUri)

        udpSender = UdpAudioSender(targetIp, port)

        var frameIndex = 0L
        var sequence = 0L
        var lastLogTime = System.currentTimeMillis()
        var packetsSkipped = 0

        Log.d(TAG, "Starting transmission in ${AudioConstants.BUFFER_DELAY_MS}ms...")

        try {
            decoder.decode { frame ->
                if (!running) return@decode

                val playAt = startTime + frameIndex * AudioConstants.FRAME_INTERVAL_MS
                val expireAt = playAt + AudioConstants.PACKET_EXPIRE_MS

                val packet = AudioPacket(
                    sequence = sequence++,
                    playAtMs = playAt,
                    expireAtMs = expireAt,
                    pcmData = frame.data
                )

                udpSender?.send(packet)

                // Pace the sending
                val sendResult = pace(frameIndex, startTime)
                if (!sendResult) {
                    packetsSkipped++
                }

                // Log progress periodically
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 5000) {
                    val elapsed = (now - startTime) / 1000.0
                    val skipped = if (packetsSkipped > 0) ", skipped=$packetsSkipped" else ""
                    Log.d(TAG, "Streaming: ${String.format("%.1f", elapsed)}s, frame=$frameIndex$skipped")
                    lastLogTime = now
                }

                frameIndex++
            }

            Log.i(TAG, "═══════════════════════════════════════════════════════")
            Log.i(TAG, "Streaming completed: $frameIndex frames sent")
            if (packetsSkipped > 0) {
                Log.w(TAG, "Warning: $packetsSkipped frames were sent late")
            }
            Log.i(TAG, "═══════════════════════════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "Sender loop failed", e)
        } finally {
            udpSender?.close()
            udpSender = null
            running = false
        }
    }

    private fun pace(frameIndex: Long, startTime: Long): Boolean {
        val expectedTime = startTime + frameIndex * AudioConstants.FRAME_INTERVAL_MS
        val now = System.currentTimeMillis()
        val delay = expectedTime - now

        if (delay > 0) {
            try {
                Thread.sleep(delay)
            } catch (_: InterruptedException) {
                return false
            }
            return true
        } else if (delay < -100) {
            // We're falling behind significantly
            Log.w(TAG, "⚠ Sender lagging ${-delay}ms behind at frame $frameIndex")
            return false
        }
        return true
    }
}