package com.example.musicsharing.domain.network.receiver

import android.annotation.SuppressLint
import android.util.Log
import com.example.musicsharing.core.constant.AudioConstants
import com.example.musicsharing.core.model.BufferedFrame
import java.util.PriorityQueue


class JitterBuffer {

    companion object {
        private const val TAG = "JitterBuffer"
    }

    private val buffer = PriorityQueue<BufferedFrame>(
        compareBy { it.playAtMs }
    )

    private var lastPlayedTime = 0L
    private var isBuffering = true
    private var playbackStartTime = 0L
    private var packetsReceived = 0
    private var packetsDroppedLate = 0
    private var packetsDroppedFull = 0
    private var packetsPlayed = 0

    @Synchronized
    fun offer(frame: BufferedFrame, nowSenderTime: Long) {
        packetsReceived++

        if (!isBuffering) {
            val lateness = nowSenderTime - frame.playAtMs
            if (lateness > AudioConstants.LATE_TOLERANCE_MS) {
                packetsDroppedLate++
                return
            }
        }

        val maxFrames = (AudioConstants.MAX_JITTER_BUFFER_MS / AudioConstants.FRAME_INTERVAL_MS).toInt()
        if (buffer.size >= maxFrames) {
            packetsDroppedFull++
            buffer.poll()  // Drop oldest
        }

        buffer.offer(frame)
    }

    @Synchronized
    fun pollReadyFrames(nowSenderTime: Long): List<BufferedFrame> {
        if (isBuffering) {
            if (buffer.size >= AudioConstants.MIN_BUFFER_FRAMES) {
                isBuffering = false

                // Set playback anchor to first packet
                val firstPacket = buffer.peek()
                if (firstPacket != null) {
                    playbackStartTime = firstPacket.playAtMs
                    lastPlayedTime = playbackStartTime - AudioConstants.FRAME_INTERVAL_MS
                    Log.i(TAG, "Buffering complete (${buffer.size} frames), starting playback")
                }
            } else {
                return emptyList()
            }
        }

        val ready = mutableListOf<BufferedFrame>()

        while (buffer.isNotEmpty()) {
            val frame = buffer.peek() ?: break

            val expectedPlayTime = lastPlayedTime + AudioConstants.FRAME_INTERVAL_MS
            val timeDiff = frame.playAtMs - expectedPlayTime

            when {
                timeDiff >= -AudioConstants.LATE_TOLERANCE_MS &&
                        timeDiff <= AudioConstants.FRAME_INTERVAL_MS * 3 -> {
                    buffer.poll()
                    ready.add(frame)
                    lastPlayedTime = frame.playAtMs
                    packetsPlayed++
                }
                timeDiff > AudioConstants.FRAME_INTERVAL_MS * 3 -> {
                    break
                }

                else -> {
                    buffer.poll()
                    packetsDroppedLate++
                }
            }
        }

        if (packetsPlayed > 0 && packetsPlayed % 1000 == 0) {
            val lossRate = if (packetsReceived > 0) {
                (packetsDroppedLate + packetsDroppedFull) * 100.0 / packetsReceived
            } else 0.0
            Log.i(TAG, "1k frames played, buffer=${buffer.size}, loss=${String.format("%.2f", lossRate)}%")
        }

        if (buffer.isEmpty() && !isBuffering && ready.isEmpty()) {
            Log.w(TAG, "Buffer underrun, re-buffering")
            isBuffering = true
            playbackStartTime = 0L
        }

        return ready
    }

    @Synchronized
    fun clear() {
        buffer.clear()
        lastPlayedTime = 0L
        playbackStartTime = 0L
        isBuffering = true
        packetsReceived = 0
        packetsDroppedLate = 0
        packetsDroppedFull = 0
        packetsPlayed = 0
    }

    @Synchronized
    fun size(): Int = buffer.size

    @Synchronized
    fun isBuffering(): Boolean = isBuffering

    @SuppressLint("DefaultLocale")
    @Synchronized
    fun getStats(): String {
        val lossRate = if (packetsReceived > 0) {
            (packetsDroppedLate + packetsDroppedFull) * 100.0 / packetsReceived
        } else 0.0
        return "received=$packetsReceived, played=$packetsPlayed, " +
                "late=$packetsDroppedLate, full=$packetsDroppedFull, " +
                "loss=${String.format("%.2f", lossRate)}%"
    }
}