package com.example.musicsharing.presentation.ui.sender

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicsharing.data.result.AudioPlaybackResult
import com.example.musicsharing.data.repository.AudioRepository
import com.example.musicsharing.data.repository.BroadcastRepository
import com.example.musicsharing.data.result.BroadcastResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch



class SenderViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private val broadcastRepository = BroadcastRepository(application)

    private val _uiState = MutableStateFlow(SenderUiState())
    val uiState: StateFlow<SenderUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SenderUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null


    fun onAudioSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(
                        selectedAudioUri = uri,
                        statusMessage = "Audio file selected - ready to play or broadcast"
                    )
                }
                _uiEvent.emit(SenderUiEvent.ShowToast("Audio selected successfully"))
            } catch (e: Exception) {
                handleError("Failed to select audio", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startBroadcast() {
        viewModelScope.launch {
            val uri = _uiState.value.selectedAudioUri
            val userName = _uiState.value.serverName


            if (uri == null) {
                _uiEvent.emit(SenderUiEvent.ShowToast("Please select audio before broadcasting"))
                return@launch
            }
            if(userName == ""){
                _uiEvent.emit(SenderUiEvent.ShowToast("Please enter server name"))
                return@launch
            }

            // Start broadcast first
            when (val result = broadcastRepository.startBroadcast(uri,userName)) {
                is BroadcastResult.Success -> {

                    releaseMediaPlayer()

                    when (val playback = audioRepository.prepareAudioPlayback(
                        uri = uri,
                        onCompletion = { onPlaybackCompleted() },
                        onError = { what, extra ->
                            handleMediaPlayerError(what, extra)
                            true
                        }
                    )) {
                        is AudioPlaybackResult.Success -> {
                            mediaPlayer = playback.mediaPlayer
                            mediaPlayer?.start()
                        }

                        is AudioPlaybackResult.Error -> {
                            _uiEvent.emit(SenderUiEvent.Error(playback.message))
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isBroadcasting = true,
                            isPlaying = true,
                            broadcastIpAddress = result.ipAddress,
                            broadcastPort = result.port,
                            statusMessage =
                                "Broadcasting & playing"
                        )
                    }

                    _uiEvent.emit(
                        SenderUiEvent.ShowToast("Broadcast started and audio playing")
                    )
                }

                is BroadcastResult.Error -> {
                    _uiEvent.emit(SenderUiEvent.Error(result.message))
                    _uiState.update { it.copy(isBroadcasting = false) }
                }
            }
        }
    }


    fun stopBroadcast() {
        viewModelScope.launch {
            releaseMediaPlayer()

            when (val result = broadcastRepository.stopBroadcast()) {
                is BroadcastResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBroadcasting = false,
                            isPlaying = false,
                            broadcastIpAddress = null,
                            broadcastPort = null,
                            statusMessage =
                                if (it.selectedAudioUri != null)
                                    "Audio file selected - ready to broadcast"
                                else
                                    "No audio file selected"
                        )
                    }
                    _uiEvent.emit(SenderUiEvent.ShowToast("Broadcast stopped"))
                }

                is BroadcastResult.Error -> {
                    _uiEvent.emit(SenderUiEvent.Error(result.message))
                }
            }
        }
    }


    private fun onPlaybackCompleted() {
        viewModelScope.launch {
            releaseMediaPlayer()
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    statusMessage = if (it.selectedAudioUri != null) {
                        "Audio file selected - ready to play or broadcast"
                    } else {
                        "No audio file selected"
                    }
                )
            }
        }
    }

    private fun handleMediaPlayerError(what: Int, extra: Int) {
        viewModelScope.launch {
            _uiEvent.emit(SenderUiEvent.Error("Playback error: code $what"))
            releaseMediaPlayer()
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    private fun handleError(message: String, exception: Exception? = null) {
        viewModelScope.launch {
            val errorMessage = exception?.localizedMessage?.let { "$message: $it" } ?: message
            _uiEvent.emit(SenderUiEvent.Error(errorMessage))
        }
    }

    private fun releaseMediaPlayer() {
        audioRepository.releaseMediaPlayer(mediaPlayer)
        mediaPlayer = null
    }

    override fun onCleared() {
        releaseMediaPlayer()
        super.onCleared()
    }

    fun onServerNameChange(it: String) {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(
                        serverName = it
                    )
                }
            }
            catch (e: Exception) {
                handleError("Failed to select audio", e)
            }
        }

    }
}
