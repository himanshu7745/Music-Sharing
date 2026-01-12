package com.example.musicsharing.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicsharing.presentation.ui.sender.SenderUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderScreen(
    uiState: SenderUiState,
    onPickAudio: () -> Unit,
    onStartBroadcast: () -> Unit,
    onStopBroadcast: () -> Unit,
    onServerNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Sender") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio Selection Section
            AudioSelectionSection(
                hasAudioSelected = uiState.selectedAudioUri != null,
                onPickAudio = onPickAudio
            )

            ServerNameSection(
                serverName = uiState.serverName,
                onServerNameChange = onServerNameChange
            )


            // Broadcast Controls Section with IP Display
            BroadcastControlsSection(
                isBroadcasting = uiState.isBroadcasting,
                hasAudioSelected = uiState.selectedAudioUri != null,
                serverName = uiState.serverName,
                ipAddress = uiState.broadcastIpAddress,
                port = uiState.broadcastPort,
                onStartBroadcast = onStartBroadcast,
                onStopBroadcast = onStopBroadcast
            )

            // Status Display
            StatusCard(statusMessage = uiState.statusMessage)
        }
    }
}

@Composable
private fun AudioSelectionSection(
    hasAudioSelected: Boolean,
    onPickAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Audio File",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Button(
                onClick = onPickAudio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(if (hasAudioSelected) "Change Music File" else "Select Music File")
            }
        }
    }
}

@Composable
private fun BroadcastControlsSection(
    isBroadcasting: Boolean,
    hasAudioSelected: Boolean,
    serverName: String,
    ipAddress: String?,
    port: Int?,
    onStartBroadcast: () -> Unit,
    onStopBroadcast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBroadcasting) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Broadcast",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isBroadcasting) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (isBroadcasting) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Broadcasting",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Broadcast Control Button
            if (!isBroadcasting) {
                Button(
                    onClick = onStartBroadcast,
                    enabled = hasAudioSelected && serverName != "",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Broadcast")
                }
            } else {
                OutlinedButton(
                    onClick = onStopBroadcast,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Stop Broadcast")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
@Composable
private fun ServerNameSection(
    serverName: String?,
    onServerNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Server Name",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (serverName != null) {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = onServerNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter server name") },
                    singleLine = true
                )
            }
        }
    }
}
