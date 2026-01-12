package com.example.musicsharing.presentation.ui.sender

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicsharing.presentation.screens.SenderScreen
import kotlinx.coroutines.flow.collectLatest

class SenderActivity : ComponentActivity() {

    private val viewModel: SenderViewModel by viewModels()

    private val audioPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            handleAudioSelection(uri)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    handleUiEvent(event)
                }
            }

            MaterialTheme {
                SenderScreen(
                    uiState = uiState,
                    onPickAudio = ::openAudioPicker,
                    onStartBroadcast = { viewModel.startBroadcast() },
                    onStopBroadcast = { viewModel.stopBroadcast() },
                    onServerNameChange = { viewModel.onServerNameChange(it) }
                )

            }
        }
    }

    private fun handleAudioSelection(uri: Uri?) {
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.onAudioSelected(it)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Failed to access audio file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAudioPicker() {
        audioPickerLauncher.launch(arrayOf("audio/*"))
    }

    private fun handleUiEvent(event: SenderUiEvent) {
        when (event) {
            is SenderUiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is SenderUiEvent.Error -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}