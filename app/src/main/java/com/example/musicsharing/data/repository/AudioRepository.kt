package com.example.musicsharing.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.musicsharing.data.result.AudioPlaybackResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioRepository(private val context: Context) {

    suspend fun prepareAudioPlayback(
        uri: Uri,
        onCompletion: () -> Unit,
        onError: (Int, Int) -> Boolean
    ): AudioPlaybackResult = withContext(Dispatchers.IO) {
        try {
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnCompletionListener { onCompletion() }
                setOnErrorListener { _, what, extra -> onError(what, extra) }
                prepare()
            }
            AudioPlaybackResult.Success(player)
        } catch (e: Exception) {
            AudioPlaybackResult.Error(e.localizedMessage ?: "Failed to prepare audio")
        }
    }

    fun releaseMediaPlayer(mediaPlayer: MediaPlayer?) {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
    }
}