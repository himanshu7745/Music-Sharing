package com.example.musicsharing.data.repository

import android.content.Context
import android.util.Log
import com.example.musicsharing.core.model.MusicServerInfo
import com.example.musicsharing.domain.network.discovery.UdpDiscoveryManager
import com.example.musicsharing.core.constant.NetworkConstants

class AudioReceiveRepository(private val context: Context) {

    companion object {
        private const val TAG = "AudioReceiveRepository"
    }

    private val discoveryManager = UdpDiscoveryManager()

    fun startDiscovery(
        onServerFound: (MusicServerInfo) -> Unit,
        discoveryDuration: Long = NetworkConstants.DISCOVERY_DURATION_MS
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

}