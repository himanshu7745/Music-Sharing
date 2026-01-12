package com.example.musicsharing.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.musicsharing.R
import com.example.musicsharing.core.constant.AppConstants.ACTION_STOP_BROADCAST
import com.example.musicsharing.core.constant.AppConstants.EXTRA_AUDIO_URI
import com.example.musicsharing.core.constant.AppConstants.EXTRA_SERVER_NAME
import com.example.musicsharing.core.constant.AppConstants.NOTIFICATION_CHANNEL_ID
import com.example.musicsharing.core.constant.AppConstants.NOTIFICATION_CHANNEL_NAME
import com.example.musicsharing.core.constant.AppConstants.NOTIFICATION_ID
import com.example.musicsharing.core.constant.NetworkConstants.DEFAULT_AUDIO_PORT
import com.example.musicsharing.core.util.NetworkUtils
import com.example.musicsharing.domain.network.sender.SenderEngine
import com.example.musicsharing.domain.network.discovery.UdpBroadcastManager

class AudioBroadcastService : Service() {

    companion object {
        private const val TAG = "AudioBroadcastService"
    }

    private var senderEngine: SenderEngine? = null
    private var udpBroadcaster: UdpBroadcastManager? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_BROADCAST -> {
                stopSelfSafely()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startSender(intent)
                return START_STICKY
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startSender(intent: Intent?) {
        val audioUriString = intent?.getStringExtra(EXTRA_AUDIO_URI)
        val serverName = intent?.getStringExtra(EXTRA_SERVER_NAME)

        if (audioUriString.isNullOrEmpty()) {
            Log.e(TAG, "No audio URI provided")
            stopSelfSafely()
            return
        }

        if(serverName.isNullOrEmpty()) {
            Log.e(TAG, "No server name provided")
            stopSelfSafely()
            return
        }

        val audioUri = audioUriString.toUri()
        val ipAddress = NetworkUtils.getLocalIpAddress(this)

        if (ipAddress.isEmpty()) {
            Log.e(TAG, "Unable to get local IP address")
            stopSelfSafely()
            return
        }

        Log.d(TAG, "Starting sender on $ipAddress:$DEFAULT_AUDIO_PORT")

        senderEngine = SenderEngine(
            context = this,
            audioUri = audioUri,
            targetIp = "255.255.255.255",
            port = DEFAULT_AUDIO_PORT
        ).apply {
            start()
        }

        udpBroadcaster = UdpBroadcastManager().apply {
            start(ipAddress, DEFAULT_AUDIO_PORT, serverName)
        }
    }

    private fun stopSelfSafely() {
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        senderEngine?.stop()
        senderEngine = null

        udpBroadcaster?.stop()
        udpBroadcaster = null

        Log.d(TAG, "Sender stopped and cleaned up")
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        createNotificationChannel()

        val stopIntent = Intent(this, AudioBroadcastService::class.java).apply {
            action = ACTION_STOP_BROADCAST
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Music Broadcast Active")
            .setContentText("Streaming audio over local Wi-Fi")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Audio broadcast is running"
        }

        val manager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}