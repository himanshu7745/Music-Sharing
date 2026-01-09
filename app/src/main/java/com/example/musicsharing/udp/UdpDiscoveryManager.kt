package com.example.musicsharing.udp

import android.util.Log
import com.example.musicsharing.utils.MusicServerInfo
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class UdpDiscoveryManager {

    companion object {
        private const val TAG = "UdpDiscoveryManager"
        private const val BROADCAST_PORT = 8888
        private const val PROTOCOL_PREFIX = "MUSIC_SERVER"
    }

    private val isRunning = AtomicBoolean(false)
    private var discoveryThread: Thread? = null
    private var socket: DatagramSocket? = null

    fun startDiscovery(
        onServerFound: (MusicServerInfo) -> Unit,
        discoveryDuration: Long = 10_000L
    ) {
        if (!isRunning.compareAndSet(false, true)) {
            Log.w(TAG, "Discovery already running")
            return
        }

        discoveryThread = Thread {
            runDiscoveryLoop(onServerFound, discoveryDuration)
        }.apply {
            name = "UdpDiscoveryThread"
            isDaemon = true
            start()
        }
    }

    private fun runDiscoveryLoop(
        onServerFound: (MusicServerInfo) -> Unit,
        discoveryDuration: Long
    ) {
        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(BROADCAST_PORT))
                soTimeout = 2000 // 2 second timeout for receive
            }

            Log.d(TAG, "UDP discovery listening on port $BROADCAST_PORT")

            val buffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()
            val discoveredServers = mutableSetOf<String>() // Track unique servers

            while (isRunning.get() &&
                (System.currentTimeMillis() - startTime) < discoveryDuration) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val message = String(
                        packet.data,
                        packet.offset,
                        packet.length,
                        Charsets.UTF_8
                    )

                    Log.d(TAG, "Received broadcast: $message from ${packet.address.hostAddress}")

                    parseServerInfo(message)?.let { serverInfo ->

                        if (discoveredServers.add(serverInfo.streamUrl)) {
                            Log.d(TAG, "New server discovered: ${serverInfo.streamUrl}")
                            onServerFound(serverInfo)
                        }
                    }

                } catch (e: java.net.SocketTimeoutException) {
                    continue
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        Log.e(TAG, "Error receiving packet", e)
                    }
                }
            }

            Log.d(TAG, "Discovery completed. Found ${discoveredServers.size} server(s)")

        } catch (e: Exception) {
            Log.e(TAG, "Error in discovery loop", e)
        } finally {
            cleanupSocket()
            isRunning.set(false)
            Log.d(TAG, "Discovery loop ended")
        }
    }


    private fun parseServerInfo(message: String): MusicServerInfo? {
        return try {
            val parts = message.split("|")

            if (parts.size >= 4 && parts[0] == PROTOCOL_PREFIX) {
                MusicServerInfo(
                    ipAddress = parts[1],
                    port = parts[2].toInt(),
                    endpoint = parts[3]
                )
            } else {
                Log.w(TAG, "Invalid broadcast message format: $message")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing broadcast message: $message", e)
            null
        }
    }

    fun stopDiscovery() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }

        Log.d(TAG, "Stopping UDP discovery")

        cleanupSocket()

        try {
            discoveryThread?.join(3000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for discovery thread to stop")
            Thread.currentThread().interrupt()
        }

        discoveryThread = null
    }

    private fun cleanupSocket() {
        socket?.close()
        socket = null
    }


}