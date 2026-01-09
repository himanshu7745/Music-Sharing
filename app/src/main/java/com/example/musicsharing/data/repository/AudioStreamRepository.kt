package com.example.musicsharing.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.musicsharing.data.result.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioStreamRepository(private val context: Context) {

    companion object {
        private const val TAG = "AudioStreamRepository"
    }
    suspend fun prepareStream(
        streamUrl: String,
        onPrepared: () -> Unit,
        onCompletion: () -> Unit,
        onError: (Int, Int) -> Boolean,
        onBufferingUpdate: (Int) -> Unit
    ): StreamResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Preparing stream from: $streamUrl")

            val player = MediaPlayer().apply {
                setDataSource(streamUrl)
                setOnPreparedListener {
                    Log.d(TAG, "Stream prepared, duration: ${it.duration}ms")
                    onPrepared()
                }
                setOnCompletionListener {
                    Log.d(TAG, "Stream completed")
                    onCompletion()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    onError(what, extra)
                }
                setOnBufferingUpdateListener { _, percent ->
                    onBufferingUpdate(percent)
                }
                // Prepare asynchronously
                prepareAsync()
            }

            StreamResult.Success(player)

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing stream", e)
            StreamResult.Error(e.localizedMessage ?: "Failed to prepare stream")
        }
    }


    fun releasePlayer(mediaPlayer: MediaPlayer?) {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            Log.d(TAG, "MediaPlayer released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        }
    }

}