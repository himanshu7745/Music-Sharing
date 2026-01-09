package com.example.musicsharing.utils

data class MusicServerInfo(
    val ipAddress: String,
    val port: Int,
    val endpoint: String,
    val streamUrl: String = "http://$ipAddress:$port$endpoint"
)
