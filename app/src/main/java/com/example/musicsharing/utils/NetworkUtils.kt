package com.example.musicsharing.utils

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter

object NetworkUtils {

    fun getLocalIpAddress(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress

            if (ipAddress != 0) {
                Formatter.formatIpAddress(ipAddress)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
    fun isWifiConnected(context: Context): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.connectionInfo.ipAddress != 0
        } catch (e: Exception) {
            false
        }
    }
}