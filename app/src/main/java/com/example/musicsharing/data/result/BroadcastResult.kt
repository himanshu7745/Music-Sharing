package com.example.musicsharing.data.result

sealed class BroadcastResult {
    data class Success(val ipAddress: String, val port: Int) : BroadcastResult()
    data class Error(val message: String) : BroadcastResult()
}