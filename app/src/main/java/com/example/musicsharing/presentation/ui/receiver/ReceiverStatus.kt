package com.example.musicsharing.presentation.ui.receiver

sealed class ReceiverStatus {
    object Idle : ReceiverStatus()
    object Discovering : ReceiverStatus()
    data class ServerFound(val count: Int) : ReceiverStatus()
    object Connecting : ReceiverStatus()
    object Connected : ReceiverStatus()
    object Playing : ReceiverStatus()
    object Paused : ReceiverStatus()
    object Buffering : ReceiverStatus()
    object Reconnecting : ReceiverStatus()
    data class Error(val message: String) : ReceiverStatus()
    object Stopped : ReceiverStatus()

    fun toDisplayString(): String = when (this) {
        is Idle -> "Idle"
        is Discovering -> "Discovering Servers..."
        is ServerFound -> "$count Server${if (count > 1) "s" else ""} Found"
        is Connecting -> "Connecting..."
        is Connected -> "Connected"
        is Playing -> "Playing"
        is Paused -> "Paused"
        is Buffering -> "Buffering..."
        is Reconnecting -> "Reconnecting..."
        is Error -> "Error: $message"
        is Stopped -> "Stopped"
    }
}