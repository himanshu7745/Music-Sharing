package com.example.musicsharing.data.result

import android.media.MediaPlayer

sealed class AudioPlaybackResult {
    data class Success(val mediaPlayer: MediaPlayer) : AudioPlaybackResult()
    data class Error(val message: String) : AudioPlaybackResult()
}