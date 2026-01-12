package com.example.musicsharing.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicsharing.core.constant.AppConstants.EXTRA_AUDIO_URI
import com.example.musicsharing.core.constant.AppConstants.EXTRA_SERVER_NAME
import com.example.musicsharing.core.constant.NetworkConstants.DEFAULT_AUDIO_PORT
import com.example.musicsharing.data.result.BroadcastResult
import com.example.musicsharing.domain.service.AudioBroadcastService
import com.example.musicsharing.core.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BroadcastRepository(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun startBroadcast(audioUri: Uri,serverName: String): BroadcastResult = withContext(Dispatchers.Main) {
        try {
            // Check WiFi connection first
            if (!NetworkUtils.isWifiConnected(context)) {
                return@withContext BroadcastResult.Error("Not connected to WiFi")
            }

            val ipAddress = NetworkUtils.getLocalIpAddress(context)
            if (ipAddress.isEmpty()) {
                return@withContext BroadcastResult.Error("Unable to get IP address")
            }

            val intent = Intent(context, AudioBroadcastService::class.java).apply {
                putExtra(EXTRA_AUDIO_URI, audioUri.toString())
                putExtra(EXTRA_SERVER_NAME,serverName)
            }
            context.startForegroundService(intent)

            BroadcastResult.Success(ipAddress, DEFAULT_AUDIO_PORT)
        } catch (e: Exception) {
            BroadcastResult.Error(e.localizedMessage ?: "Failed to start broadcast")
        }
    }

    suspend fun stopBroadcast(): BroadcastResult = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(context, AudioBroadcastService::class.java)
            context.stopService(intent)

            val ipAddress = NetworkUtils.getLocalIpAddress(context)
            BroadcastResult.Success(ipAddress, DEFAULT_AUDIO_PORT)
        } catch (e: Exception) {
            BroadcastResult.Error(e.localizedMessage ?: "Failed to stop broadcast")
        }
    }
}