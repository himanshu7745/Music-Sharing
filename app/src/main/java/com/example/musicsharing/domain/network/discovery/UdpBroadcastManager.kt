package com.example.musicsharing.domain.network.discovery

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import com.example.musicsharing.core.constant.NetworkConstants.BROADCAST_PORT
import com.example.musicsharing.core.constant.NetworkConstants.BROADCAST_ADDRESS
import com.example.musicsharing.core.constant.NetworkConstants.BROADCAST_INTERVAL_MS
import com.example.musicsharing.core.constant.NetworkConstants.THREAD_JOIN_TIMEOUT_MS
import com.example.musicsharing.core.constant.NetworkConstants.PROTOCOL_PREFIX

class UdpBroadcastManager {

    companion object {
        private const val TAG = "UdpBroadcastManager"
    }

    private val isRunning = AtomicBoolean(false)
    private var broadcastThread: Thread? = null
    private var socket: DatagramSocket? = null

    fun start(ipAddress: String, port: Int, serverName: String) {
        if (!isRunning.compareAndSet(false, true)) {
            Log.w(TAG, "Broadcast already running")
            return
        }

        broadcastThread = Thread {
            runBroadcastLoop(ipAddress, port, serverName)
        }.apply {
            name = "UdpBroadcastThread"
            isDaemon = true
            start()
        }
    }

    private fun runBroadcastLoop(ipAddress: String, port: Int, serverName: String) {
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                reuseAddress = true
            }

            val broadcastAddress = InetAddress.getByName(BROADCAST_ADDRESS)
            Log.d(TAG, "Broadcast socket created")

            val message = createBroadcastMessage(ipAddress, port, serverName)
            val data = message.toByteArray(Charsets.UTF_8)

            Log.d(TAG, "Starting UDP broadcast: $message to $BROADCAST_ADDRESS:$BROADCAST_PORT")

            while (isRunning.get()) {
                try {
                    val packet = DatagramPacket(
                        data,
                        data.size,
                        broadcastAddress,
                        BROADCAST_PORT
                    )

                    socket?.send(packet)
                    Log.v(TAG, "Broadcast packet sent")

                    Thread.sleep(BROADCAST_INTERVAL_MS)

                } catch (e: InterruptedException) {
                    Log.d(TAG, "Broadcast thread interrupted")
                    break
                } catch (e: IOException) {
                    if (isRunning.get()) {
                        Log.e(TAG, "Error sending broadcast packet", e)
                        Thread.sleep(1000)
                    }
                }
            }

        } catch (e: SocketException) {
            Log.e(TAG, "Failed to create broadcast socket", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in broadcast loop", e)
        } finally {
            cleanupSocket()
            Log.d(TAG, "Broadcast loop ended")
        }
    }

    private fun createBroadcastMessage(ipAddress: String, port: Int, serverName: String): String {
        return "$PROTOCOL_PREFIX|$ipAddress|$port|$serverName"
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            return
        }

        Log.d(TAG, "Stopping UDP broadcast")

        broadcastThread?.interrupt()
        cleanupSocket()

        try {
            broadcastThread?.join(THREAD_JOIN_TIMEOUT_MS)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for thread to stop")
            Thread.currentThread().interrupt()
        }

        broadcastThread = null
    }

    private fun cleanupSocket() {
        socket?.close()
        socket = null
    }


}