package com.example.musicsharing.ui.receiver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicsharing.ui.screens.ReceiverScreen
import com.example.musicsharing.ui.receiver.ReceiverUiEvent
import kotlinx.coroutines.flow.collectLatest

class ReceiverActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Permissions required for discovering servers",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            MaterialTheme {
                val viewModel: ReceiverViewModel = viewModel(
                    factory = ReceiverViewModelFactory(this)
                )

                val uiState = viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { event ->
                        handleUiEvent(event)
                    }
                }

                ReceiverScreen(
                    uiState = uiState.value,
                    onStartDiscovery = { viewModel.startDiscovery() },
                    onStopDiscovery = { viewModel.stopDiscovery() },
                    onSelectServer = { server -> viewModel.selectServer(server) },
                    onConnectAndPlay = { viewModel.connectAndPlay() },
                    onPause = { viewModel.pausePlayback() },
                    onResume = { viewModel.resumePlayback() },
                    onStop = { viewModel.stopPlayback() },
                    onSeek = { position -> viewModel.seekTo(position) }
                )
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // Required for WiFi multicast (UDP broadcasts)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_WIFI_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun handleUiEvent(event: ReceiverUiEvent) {
        when (event) {
            is ReceiverUiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is ReceiverUiEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}