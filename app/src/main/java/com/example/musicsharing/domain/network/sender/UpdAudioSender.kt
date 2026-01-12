package com.example.musicsharing.domain.network.sender

import android.util.Log
import com.example.musicsharing.core.constant.NetworkConstants.IPTOS_RELIABILITY
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_FLAGS
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_HEADER_SIZE
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_MAGIC
import com.example.musicsharing.core.constant.NetworkConstants.PACKET_VERSION
import com.example.musicsharing.core.constant.NetworkConstants.UDP_SEND_BUFFER_SIZE
import com.example.musicsharing.core.model.AudioPacket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UdpAudioSender(
    ip: String,
    port: Int
) {

    companion object {
        private const val TAG = "UdpAudioSender"
    }

    private val socket = DatagramSocket().apply {
        sendBufferSize = UDP_SEND_BUFFER_SIZE
        trafficClass = IPTOS_RELIABILITY

        Log.d(TAG, "UDP socket created, send buffer: $sendBufferSize bytes")
    }

    private val address = InetSocketAddress(ip, port)

    private var packetsSent = 0L
    private var bytesSent = 0L
    private var sendErrors = 0

    fun send(packet: AudioPacket) {
        try {
            val data = serialize(packet)
            val datagram = DatagramPacket(data, data.size, address)
            socket.send(datagram)

            packetsSent++
            bytesSent += data.size

            // Log stats periodically
            if (packetsSent % 500 == 0L) {
                Log.i(TAG, "Sent $packetsSent packets, ${bytesSent / 1024}KB, errors: $sendErrors")
            }
        } catch (e: Exception) {
            sendErrors++
            Log.e(TAG, "Failed to send packet #${packet.sequence}", e)
        }
    }

    private fun serialize(packet: AudioPacket): ByteArray {
        val pcmLength = packet.pcmData.size
        require(pcmLength <= Short.MAX_VALUE) {
            "PCM frame too large: $pcmLength"
        }

        val buffer = ByteBuffer
            .allocate(PACKET_HEADER_SIZE + pcmLength)
            .order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(PACKET_MAGIC)
        buffer.put(PACKET_VERSION)
        buffer.put(PACKET_FLAGS)

        buffer.putLong(packet.sequence)
        buffer.putLong(packet.playAtMs)
        buffer.putLong(packet.expireAtMs)

        buffer.putShort(pcmLength.toShort())
        buffer.put(packet.pcmData)

        return buffer.array()
    }

    fun close() {
        Log.d(TAG, "Closing sender. Total sent: $packetsSent packets, errors: $sendErrors")
        socket.close()
    }
}