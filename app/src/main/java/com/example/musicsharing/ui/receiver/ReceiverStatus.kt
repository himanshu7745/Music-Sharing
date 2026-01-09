package com.example.musicsharing.ui.receiver

sealed class ReceiverStatus {
    object Idle : ReceiverStatus()
    object Discovering : ReceiverStatus()
    data class ServerFound(val count: Int) : ReceiverStatus()
    object Connecting : ReceiverStatus()
    object Playing : ReceiverStatus()
    object Paused : ReceiverStatus()
    data class Error(val message: String) : ReceiverStatus()

    fun toDisplayString(): String = when (this) {
        is Idle -> "Ready to discover servers"
        is Discovering -> "Searching for music servers..."
        is ServerFound -> "Found $count server(s)"
        is Connecting -> "Connecting to server..."
        is Playing -> "Playing"
        is Paused -> "Paused"
        is Error -> "Error: $message"
    }
}