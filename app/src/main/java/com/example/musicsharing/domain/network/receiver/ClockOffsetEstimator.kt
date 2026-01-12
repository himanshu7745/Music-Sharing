package com.example.musicsharing.domain.network.receiver

import android.util.Log
import com.example.musicsharing.core.constant.AppConstants.CLOCK_SYNC_ALPHA
import com.example.musicsharing.core.constant.AppConstants.CLOCK_SYNC_MIN_SAMPLES

class ClockOffsetEstimator {

    companion object {
        private const val TAG = "ClockOffsetEstimator"
    }

    @Volatile
    private var estimatedOffset = 0L

    private var sampleCount = 0
    private val recentOffsets = mutableListOf<Long>()

    @Synchronized
    fun update(senderTime: Long, localTime: Long) {
        val offset = senderTime - localTime

        recentOffsets.add(offset)
        if (recentOffsets.size > 10) {
            recentOffsets.removeAt(0)
        }

        sampleCount++

        if (sampleCount < CLOCK_SYNC_MIN_SAMPLES) {
            // Bootstrap: use simple average for first few samples
            estimatedOffset = recentOffsets.average().toLong()
        } else {
            // Use exponential moving average for smooth tracking
            // This adapts to clock drift while filtering out noise
            estimatedOffset = (CLOCK_SYNC_ALPHA * offset + (1 - CLOCK_SYNC_ALPHA) * estimatedOffset).toLong()
        }

        if (sampleCount % 100 == 0) {
            val jitter = if (recentOffsets.size > 1) {
                recentOffsets.maxOrNull()!! - recentOffsets.minOrNull()!!
            } else 0
            Log.d(TAG, "Clock offset: ${estimatedOffset}ms, jitter: ${jitter}ms, samples: $sampleCount")
        }
    }
    fun toSenderTime(localTime: Long): Long {
        return localTime + estimatedOffset
    }

    fun getOffset(): Long = estimatedOffset

    @Synchronized
    fun reset() {
        estimatedOffset = 0L
        sampleCount = 0
        recentOffsets.clear()
    }
}