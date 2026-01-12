package com.example.musicsharing.core.util

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.musicsharing.core.constant.AppConstants

class WiFiMulticastHelper(private val context: Context) {

    companion object {
        private const val TAG = "WiFiMulticastHelper"
    }

    private var multicastLock: WifiManager.MulticastLock? = null


    fun acquire() {
        try {
            if (multicastLock?.isHeld == true) {
                Log.d(TAG, "Multicast lock already held")
                return
            }

            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            multicastLock = wifiManager.createMulticastLock(AppConstants.MULTICAST_LOCK_TAG).apply {
                setReferenceCounted(true)
                acquire()
            }

            Log.d(TAG, "Multicast lock acquired")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire multicast lock", e)
        }
    }

    fun release() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "Multicast lock released")
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release multicast lock", e)
        }
    }


}