package com.example.musicsharing.data.repository

import android.content.Context
import android.util.Log
import com.example.musicsharing.utils.MusicServerInfo
import com.example.musicsharing.udp.UdpDiscoveryManager

class AudioReceiveRepository(private val context: Context) {

    companion object {
        private const val TAG = "AudioReceiveRepository"
    }

    private val discoveryManager = UdpDiscoveryManager()
    private val streamRepository = AudioStreamRepository(context)

    fun startDiscovery(
        onServerFound: (MusicServerInfo) -> Unit,
        discoveryDuration: Long = 10_000L
    ) {
        Log.d(TAG, "Starting server discovery")
        discoveryManager.startDiscovery(
            onServerFound = onServerFound,
            discoveryDuration = discoveryDuration
        )
    }

    fun stopDiscovery() {
        Log.d(TAG, "Stopping server discovery")
        discoveryManager.stopDiscovery()
    }

    fun getStreamRepository(): AudioStreamRepository {
        return streamRepository
    }
}