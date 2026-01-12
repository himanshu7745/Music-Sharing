package com.example.musicsharing.domain.audio.packet

import android.util.Log
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_HEADER_SIZE
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_MAGIC
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_VERSION
import com.example.musicsharing.core.model.AudioPacket
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioPacketParser {

    private const val TAG = "AudioPacketParser"

    fun parse(data: ByteArray, length: Int): AudioPacket? {
        if (length < PACKET_HEADER_SIZE) {
            Log.w(TAG, "Packet too small: $length bytes")
            return null
        }

        val buffer = ByteBuffer.wrap(data, 0, length)
            .order(ByteOrder.BIG_ENDIAN)

        // Validate magic number
        val magic = buffer.getShort()
        if (magic != PACKET_MAGIC) {
            Log.w(TAG, "Invalid magic number: ${magic.toString(16)}")
            return null
        }

        // Validate version
        val version = buffer.get()
        if (version != PACKET_VERSION) {
            Log.w(TAG, "Unsupported version: $version")
            return null
        }

        // Skip flags
        buffer.get()

        // Read packet data
        val sequence = buffer.getLong()
        val playAtMs = buffer.getLong()
        val expireAtMs = buffer.getLong()
        val pcmLength = buffer.getShort().toInt() and 0xFFFF

        // Validate PCM length
        val expectedLength = PACKET_HEADER_SIZE + pcmLength
        if (length < expectedLength) {
            Log.w(TAG, "Incomplete packet: expected $expectedLength, got $length")
            return null
        }

        // Extract PCM data
        val pcmData = ByteArray(pcmLength)
        buffer.get(pcmData)

        return AudioPacket(
            sequence = sequence,
            playAtMs = playAtMs,
            expireAtMs = expireAtMs,
            pcmData = pcmData
        )
    }
}