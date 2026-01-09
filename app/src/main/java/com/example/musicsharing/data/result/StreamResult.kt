package com.example.musicsharing.data.result

import android.media.MediaPlayer

sealed class StreamResult {
    data class Success(val mediaPlayer: MediaPlayer) : StreamResult()
    data class Error(val message: String) : StreamResult()
}