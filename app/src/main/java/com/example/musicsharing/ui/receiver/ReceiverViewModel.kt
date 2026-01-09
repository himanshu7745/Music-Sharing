package com.example.musicsharing.ui.receiver

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicsharing.utils.MusicServerInfo
import com.example.musicsharing.data.repository.AudioReceiveRepository
import com.example.musicsharing.data.result.StreamResult
import com.example.musicsharing.utils.WiFiMulticastHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ReceiverViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "ReceiverViewModel"
    }

    private val repository = AudioReceiveRepository(context)
    private val streamRepository = repository.getStreamRepository()
    private val multicastHelper = WiFiMulticastHelper(context)

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ReceiverUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressUpdateJob: Job? = null

    fun startDiscovery() {
        viewModelScope.launch {
            try {
                multicastHelper.acquire()

                _uiState.update { it.copy(
                    status = ReceiverStatus.Discovering,
                    discoveredServers = emptyList(),
                    selectedServer = null
                )}

                repository.startDiscovery(
                    onServerFound = { serverInfo ->
                        handleServerFound(serverInfo)
                    },
                    discoveryDuration = 10_000L
                )

                delay(10_500L)
                val serverCount = _uiState.value.discoveredServers.size

                if (serverCount > 0) {
                    _uiState.update { it.copy(
                        status = ReceiverStatus.ServerFound(serverCount)
                    )}
                    _uiEvent.emit(ReceiverUiEvent.ShowToast("Found $serverCount server(s)"))
                } else {
                    _uiState.update { it.copy(
                        status = ReceiverStatus.Idle
                    )}
                    _uiEvent.emit(ReceiverUiEvent.ShowToast("No servers found"))
                }

            } catch (e: Exception) {
                handleError("Discovery failed: ${e.localizedMessage}")
            }
        }
    }

    fun stopDiscovery() {
        repository.stopDiscovery()
        multicastHelper.release()
        _uiState.update { it.copy(
            status = if (it.discoveredServers.isEmpty()) {
                ReceiverStatus.Idle
            } else {
                ReceiverStatus.ServerFound(it.discoveredServers.size)
            }
        )}
    }

    private fun handleServerFound(serverInfo: MusicServerInfo) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val updatedServers = (currentState.discoveredServers + serverInfo).distinctBy { it.streamUrl }
                currentState.copy(
                    discoveredServers = updatedServers,
                    status = ReceiverStatus.ServerFound(updatedServers.size)
                )
            }
            Log.d(TAG, "Server found: ${serverInfo.streamUrl}")
        }
    }

    fun selectServer(server: MusicServerInfo) {
        _uiState.update { it.copy(selectedServer = server) }
    }

    fun connectAndPlay() {
        val server = _uiState.value.selectedServer
        if (server == null) {
            viewModelScope.launch {
                _uiEvent.emit(ReceiverUiEvent.ShowToast("Please select a server first"))
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    status = ReceiverStatus.Connecting,
                    isLoading = true
                )}

                releaseMediaPlayer()

                when (val result = streamRepository.prepareStream(
                    streamUrl = server.streamUrl,
                    onPrepared = {
                        handleStreamPrepared()
                    },
                    onCompletion = {
                        handleStreamCompleted()
                    },
                    onError = { what, extra ->
                        handleStreamError(what, extra)
                        true
                    },
                    onBufferingUpdate = { percent ->
                        Log.d(TAG, "Buffering: $percent%")
                    }
                )) {
                    is StreamResult.Success -> {
                        mediaPlayer = result.mediaPlayer
                        Log.d(TAG, "Stream prepared successfully")
                    }
                    is StreamResult.Error -> {
                        handleError(result.message)
                    }
                }

            } catch (e: Exception) {
                handleError("Connection failed: ${e.localizedMessage}")
            }
        }
    }

    private fun handleStreamPrepared() {
        viewModelScope.launch {
            try {
                mediaPlayer?.start()

                _uiState.update { it.copy(
                    status = ReceiverStatus.Playing,
                    isPlaying = true,
                    isLoading = false,
                    duration = mediaPlayer?.duration?.toLong() ?: 0L
                )}

                startProgressUpdates()
                _uiEvent.emit(ReceiverUiEvent.ShowToast("Playing audio"))

            } catch (e: Exception) {
                handleError("Playback failed: ${e.localizedMessage}")
            }
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.pause()
            _uiState.update { it.copy(
                isPlaying = false,
                status = ReceiverStatus.Paused
            )}
            stopProgressUpdates()
        } catch (e: Exception) {
            handleError("Pause failed: ${e.localizedMessage}")
        }
    }

    fun resumePlayback() {
        try {
            mediaPlayer?.start()
            _uiState.update { it.copy(
                isPlaying = true,
                status = ReceiverStatus.Playing
            )}
            startProgressUpdates()
        } catch (e: Exception) {
            handleError("Resume failed: ${e.localizedMessage}")
        }
    }

    fun stopPlayback() {
        releaseMediaPlayer()
        _uiState.update { it.copy(
            isPlaying = false,
            currentPosition = 0L,
            status = if (it.discoveredServers.isEmpty()) {
                ReceiverStatus.Idle
            } else {
                ReceiverStatus.ServerFound(it.discoveredServers.size)
            }
        )}
    }

    fun seekTo(position: Long) {
        try {
            mediaPlayer?.seekTo(position.toInt())
            _uiState.update { it.copy(currentPosition = position) }
        } catch (e: Exception) {
            Log.e(TAG, "Seek failed", e)
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = viewModelScope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                try {
                    val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _uiState.update { it.copy(currentPosition = position) }
                    delay(100) // Update every 100ms
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating progress", e)
                    break
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    private fun handleStreamCompleted() {
        viewModelScope.launch {
            stopPlayback()
            _uiEvent.emit(ReceiverUiEvent.ShowToast("Playback completed"))
        }
    }

    private fun handleStreamError(what: Int, extra: Int) {
        viewModelScope.launch {
            handleError("Stream error: code $what")
        }
    }

    private fun handleError(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                status = ReceiverStatus.Error(message),
                isLoading = false,
                isPlaying = false
            )}
            _uiEvent.emit(ReceiverUiEvent.ShowError(message))
        }
    }

    private fun releaseMediaPlayer() {
        stopProgressUpdates()
        streamRepository.releasePlayer(mediaPlayer)
        mediaPlayer = null
    }

    override fun onCleared() {
        releaseMediaPlayer()
        repository.stopDiscovery()
        multicastHelper.release()
        super.onCleared()
    }


}