package com.example.musicsharing.domain.network.receiver

import android.util.Log
import com.example.musicsharing.core.constant.NetworkConstants
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class UdpAudioReceiver(
    private val port: Int,
    private val onPacketReceived: (ByteArray, Int) -> Unit
) {

    companion object {
        private const val TAG = "UdpAudioReceiver"
    }

    private val running = AtomicBoolean(false)
    private var socket: DatagramSocket? = null
    private var receiveThread: Thread? = null
    private var packetsReceived = 0L
    private var bytesReceived = 0L

    fun start() {
        if (!running.compareAndSet(false, true)) {
            Log.w(TAG, "Receiver already running")
            return
        }

        receiveThread = Thread {
            receiveLoop()
        }.apply {
            name = "UdpAudioReceiverThread"
            priority = Thread.MAX_PRIORITY  // High priority for network reception
            isDaemon = true
            start()
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) {
            return
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        try {
            receiveThread?.interrupt()
            receiveThread?.join(2000)
        } catch (_: InterruptedException) {
        }

        receiveThread = null
        socket = null

        Log.d(TAG, "Stopped. Total packets: $packetsReceived, bytes: $bytesReceived")
    }

    private fun receiveLoop() {
        try {
            socket = DatagramSocket(port).apply {
                reuseAddress = true
                // Increase socket buffer to reduce packet loss
                receiveBufferSize = NetworkConstants.UDP_SOCKET_BUFFER_SIZE


                soTimeout = 1000

                Log.d(TAG, "Actual receive buffer size: $receiveBufferSize bytes")
            }

            Log.d(TAG, "UDP receiver listening on port $port")

            val buffer = ByteArray(NetworkConstants.MAX_PACKET_SIZE)

            while (running.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    packetsReceived++
                    bytesReceived += packet.length

                    val data = packet.data.copyOf(packet.length)

                    onPacketReceived(data, packet.length)

                    // Log stats periodically
                    if (packetsReceived % 500 == 0L) {
                        Log.i(TAG, "Received $packetsReceived packets, ${bytesReceived / 1024}KB total")
                    }

                }
                catch (e: SocketException) {
                    if (running.get()) {
                        Log.e(TAG, "Socket error", e)
                    }
                    break
                } catch (e: Exception) {
                    if (running.get()) {
                        Log.e(TAG, "Error receiving packet", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UDP receiver", e)
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }

            Log.d(TAG, "UDP receiver stopped")
        }
    }
}