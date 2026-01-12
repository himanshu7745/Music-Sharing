package com.example.musicsharing.core.util

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NetworkUtils {

    fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt == 0) return ""

            val bytes = ByteBuffer
                .allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(ipInt)
                .array()

            InetAddress.getByAddress(bytes).hostAddress ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    fun isWifiConnected(context: Context): Boolean {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.connectionInfo.ipAddress != 0
        } catch (e: Exception) {
            false
        }
    }
}