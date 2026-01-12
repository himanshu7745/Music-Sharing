package com.example.musicsharing.presentation.ui.receiver

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicsharing.core.constant.NetworkConstants.DISCOVERY_DURATION_MS
import com.example.musicsharing.core.constant.NetworkConstants.DISCOVERY_TIMEOUT_MS
import com.example.musicsharing.core.model.MusicServerInfo
import com.example.musicsharing.data.repository.AudioReceiveRepository
import com.example.musicsharing.domain.network.receiver.ReceiverEngine
import com.example.musicsharing.core.util.WiFiMulticastHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReceiverViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "ReceiverViewModel"
    }

    private val repository = AudioReceiveRepository(context)
    private val multicastHelper = WiFiMulticastHelper(context)

    private val _uiState = MutableStateFlow(ReceiverUiState())
    val uiState: StateFlow<ReceiverUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ReceiverUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var receiverEngine: ReceiverEngine? = null

    fun startDiscovery() {
        viewModelScope.launch {
            try {
                multicastHelper.acquire()

                _uiState.update {
                    it.copy(
                        status = ReceiverStatus.Discovering,
                        discoveredServers = emptyList(),
                        selectedServer = null,
                        discoveryProgress = 0f,
                        lastDiscoveryTime = System.currentTimeMillis()
                    )
                }

                // Start discovery with progress updates
                repository.startDiscovery(
                    onServerFound = { serverInfo ->
                        handleServerFound(serverInfo)
                    },
                    discoveryDuration = DISCOVERY_DURATION_MS
                )

                // Simulate progress
                var progress = 0f
                while (progress < 1f && _uiState.value.status is ReceiverStatus.Discovering) {
                    delay(100)
                    progress += 0.01f
                    _uiState.update { it.copy(discoveryProgress = progress.coerceAtMost(1f)) }
                }

                delay(DISCOVERY_TIMEOUT_MS - DISCOVERY_DURATION_MS)

                val serverCount = _uiState.value.discoveredServers.size

                if (serverCount > 0) {
                    _uiState.update {
                        it.copy(
                            status = ReceiverStatus.ServerFound(serverCount),
                            totalServersDiscovered = it.totalServersDiscovered + serverCount,
                            discoveryProgress = 1f
                        )
                    }
                    _uiEvent.emit(ReceiverUiEvent.ShowToast("Found $serverCount server(s)"))
                } else {
                    _uiState.update {
                        it.copy(
                            status = ReceiverStatus.Idle,
                            discoveryProgress = 1f
                        )
                    }
                    _uiEvent.emit(ReceiverUiEvent.ShowToast("No servers found"))
                }

            } catch (e: Exception) {
                handleError("Discovery failed: ${e.localizedMessage}")
            } finally {
                multicastHelper.release()
            }
        }
    }

    fun stopDiscovery() {
        repository.stopDiscovery()
        multicastHelper.release()

        _uiState.update {
            it.copy(
                status = if (it.discoveredServers.isEmpty()) {
                    ReceiverStatus.Idle
                } else {
                    ReceiverStatus.ServerFound(it.discoveredServers.size)
                },
                discoveryProgress = 0f
            )
        }
    }

    private fun handleServerFound(serverInfo: MusicServerInfo) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val updatedServers = (currentState.discoveredServers + serverInfo)
                    .distinctBy { it.serverName }
                    .sortedByDescending { it.getQualityRating() }

                currentState.copy(
                    discoveredServers = updatedServers,
                    status = ReceiverStatus.ServerFound(updatedServers.size)
                )
            }
            Log.d(TAG, "Server found: ${serverInfo.displayName} at ${serverInfo.connectionUrl}")
        }
    }

    fun selectServer(server: MusicServerInfo) {
        _uiState.update { it.copy(selectedServer = server) }
        Log.d(TAG, "Server selected: ${server.displayName}")
    }

    fun connectAndListen() {
        val server = _uiState.value.selectedServer ?: return

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        status = ReceiverStatus.Connecting,
                        isLoading = true
                    )
                }

                // Stop any existing receiver
                receiverEngine?.stop()

                // Create and start new receiver
                receiverEngine = ReceiverEngine(server.port).apply { start() }

                _uiState.update {
                    it.copy(
                        status = ReceiverStatus.Playing,
                        isPlaying = true,
                        isLoading = false,
                        isConnected = true,
                        sessionStartTime = System.currentTimeMillis(),
                        reconnectAttempts = 0
                    )
                }

                _uiEvent.emit(
                    ReceiverUiEvent.ShowToast("Connected to ${server.displayName}")
                )

            } catch (e: Exception) {
                handleError("Connection failed: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        viewModelScope.launch {
            receiverEngine?.stop()
            receiverEngine = null

            _uiState.update {
                it.copy(
                    status = if (it.discoveredServers.isEmpty()) {
                        ReceiverStatus.Idle
                    } else {
                        ReceiverStatus.ServerFound(it.discoveredServers.size)
                    },
                    isPlaying = false,
                    isConnected = false,
                    sessionStartTime = null
                )
            }

            _uiEvent.emit(ReceiverUiEvent.ShowToast("Stopped listening"))
        }
    }

    fun setVolume(volume: Float) {
        _uiState.update {
            it.copy(volume = volume.coerceIn(0f, 1f))
        }
    }

    fun toggleMute() {
        _uiState.update {
            it.copy(isMuted = !it.isMuted)
        }
    }

    fun toggleAutoReconnect() {
        _uiState.update {
            it.copy(autoReconnect = !it.autoReconnect)
        }
    }

    fun toggleVisualization() {
        _uiState.update {
            it.copy(showVisualization = !it.showVisualization)
        }
    }

    fun toggleServerDetails() {
        _uiState.update {
            it.copy(showServerDetails = !it.showServerDetails)
        }
    }

    fun toggleAudioStats() {
        _uiState.update {
            it.copy(showAudioStats = !it.showAudioStats)
        }
    }

    private fun handleError(message: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = ReceiverStatus.Error(message),
                    isLoading = false,
                    isPlaying = false,
                    isConnected = false
                )
            }
            _uiEvent.emit(ReceiverUiEvent.ShowError(message))
            Log.e(TAG, message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        receiverEngine?.stop()
        repository.stopDiscovery()
        multicastHelper.release()
        Log.d(TAG, "ViewModel cleared")
    }
}