package com.example.musicsharing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.musicsharing.R
import com.example.musicsharing.server.AudioHttpServer
import com.example.musicsharing.udp.UdpBroadcastManager
import fi.iki.elonen.NanoHTTPD
import androidx.core.net.toUri

class AudioBroadcastService : Service() {

    companion object {
        private const val TAG = "AudioBroadcastService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "audio_broadcast_channel"
        const val DEFAULT_PORT = 49152
        private const val ACTION_STOP = "com.example.musicsharing.STOP_BROADCAST"

        const val EXTRA_AUDIO_URI = "extra_audio_uri"
    }

    private var audioServer: AudioHttpServer? = null
    private var udpBroadcaster: UdpBroadcastManager? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service starting")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        val audioUriString = intent?.getStringExtra(EXTRA_AUDIO_URI)
        if (audioUriString == null) {
            Log.e(TAG, "No audio URI provided, stopping service")
            stopSelfSafely()
            return START_NOT_STICKY
        }

        val audioUri = audioUriString.toUri()
        startBroadcasting(audioUri)

        return START_STICKY
    }

    private fun startBroadcasting(audioUri: Uri) {
        try {
            val port = DEFAULT_PORT

            audioServer = AudioHttpServer(
                port = port,
                resolver = contentResolver,
                audioUri = audioUri
            ).apply {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }

            Log.d(TAG, "HTTP server started on port $port")

            val ipAddress = getLocalIpAddress()
            if (ipAddress.isNotEmpty()) {
                udpBroadcaster = UdpBroadcastManager().apply {
                    start(ipAddress, port)
                }
                Log.d(TAG, "UDP broadcast started for $ipAddress:$port")
            } else {
                Log.e(TAG, "Unable to get local IP address")
                stopSelfSafely()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start broadcasting", e)
            stopSelfSafely()
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress != 0) {
                Formatter.formatIpAddress(ipAddress)
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
            ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        createNotificationChannel()

        val stopIntent = Intent(this, AudioBroadcastService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Broadcast Active")
            .setContentText("Sharing music on local network")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Broadcast",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Displays when music is being broadcast"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun stopSelfSafely() {
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        udpBroadcaster?.stop()
        udpBroadcaster = null

        audioServer?.stop()
        audioServer = null

        Log.d(TAG, "Service cleanup completed")
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

}