package com.example.musicsharing.presentation.ui.receiver

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicsharing.core.constant.AppConstants.NOTIFICATION_CHANNEL_ID
import com.example.musicsharing.core.constant.AppConstants.WAKE_LOCK_TAG
import com.example.musicsharing.core.constant.AppConstants.WAKE_LOCK_TIMEOUT_MS
import com.example.musicsharing.core.model.MusicServerInfo
import com.example.musicsharing.presentation.screens.ReceiverScreen
import com.example.musicsharing.presentation.ui.theme.MusicSharingTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReceiverActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filterValues { !it }.keys

        if (deniedPermissions.isNotEmpty()) {
            val message = buildString {
                append("Missing permissions:\n")
                deniedPermissions.forEach { permission ->
                    append("• ${getPermissionName(permission)}\n")
                }
            }

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()

            if (shouldShowRationale(deniedPermissions)) {
                showPermissionRationaleDialog()
            }
        } else {
            Toast.makeText(
                this,
                "All permissions granted!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission denied. You won't receive playback notifications.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Request necessary permissions
        requestPermissions()

        setContent {
            MusicSharingTheme {
                val viewModel: ReceiverViewModel = viewModel(
                    factory = ReceiverViewModelFactory(this)
                )

                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Handle UI events
                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest @RequiresPermission(android.Manifest.permission.VIBRATE) { event ->
                        handleUiEvent(event, snackbarHostState)
                    }
                }

                // Handle wake lock based on playback state
                LaunchedEffect(uiState.isPlaying, uiState.keepScreenOn) {
                    if (uiState.isPlaying && uiState.keepScreenOn) {
                        acquireWakeLock()
                    } else {
                        releaseWakeLock()
                    }
                }

                // Update window flags based on keep screen on preference
                LaunchedEffect(uiState.keepScreenOn) {
                    if (uiState.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                ReceiverScreenContainer(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onStartDiscovery = {
                        if (hasRequiredPermissions()) {
                            viewModel.startDiscovery()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Please grant required permissions",
                                    actionLabel = "Grant"
                                )
                            }
                            requestPermissions()
                        }
                    },
                    onStopDiscovery = { viewModel.stopDiscovery() },
                    onSelectServer = { server -> viewModel.selectServer(server) },
                    onConnectAndListen = { viewModel.connectAndListen() },
                    onStopListening = { viewModel.stopListening() },
                    onVolumeChange = { volume -> viewModel.setVolume(volume) },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleAutoReconnect = { viewModel.toggleAutoReconnect() },
                    onToggleVisualization = { viewModel.toggleVisualization() },
                    onToggleServerDetails = { viewModel.toggleServerDetails() },
                    onToggleAudioStats = { viewModel.toggleAudioStats() },
                    onRequestPermissions = { requestPermissions() }
                )
            }
        }
    }

    @Composable
    private fun ReceiverScreenContainer(
        uiState: ReceiverUiState,
        snackbarHostState: SnackbarHostState,
        onStartDiscovery: () -> Unit,
        onStopDiscovery: () -> Unit,
        onSelectServer: (MusicServerInfo) -> Unit,
        onConnectAndListen: () -> Unit,
        onStopListening: () -> Unit,
        onVolumeChange: (Float) -> Unit,
        onToggleMute: () -> Unit,
        onToggleAutoReconnect: () -> Unit,
        onToggleVisualization: () -> Unit,
        onToggleServerDetails: () -> Unit,
        onToggleAudioStats: () -> Unit,
        onRequestPermissions: () -> Unit
    ) {
        androidx.compose.material3.Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            ReceiverScreen(
                uiState = uiState,
                onStartDiscovery = onStartDiscovery,
                onStopDiscovery = onStopDiscovery,
                onSelectServer = onSelectServer,
                onConnectAndListen = onConnectAndListen,
                onStopListening = onStopListening,
                modifier = androidx.compose.ui.Modifier.padding(paddingValues)
            )
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // WiFi state access (required for network discovery)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        }

        // Network state (helpful for connection monitoring)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE)
        }

        // Location permission for WiFi scanning on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WAKE_LOCK
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.WAKE_LOCK)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val requiredPermissions = listOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun shouldShowRationale(deniedPermissions: Set<String>): Boolean {
        return deniedPermissions.any { permission ->
            shouldShowRequestPermissionRationale(permission)
        }
    }

    private fun showPermissionRationaleDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(
                "This app needs certain permissions to discover and connect to music servers:\n\n" +
                        "• WiFi State: To detect available servers on your network\n" +
                        "• Network State: To monitor connection quality\n" +
                        "• Location: Required for WiFi scanning on Android 10+\n\n" +
                        "Would you like to open settings to grant these permissions?"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_WIFI_STATE -> "WiFi State Access"
            Manifest.permission.ACCESS_NETWORK_STATE -> "Network State Access"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location Access"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.WAKE_LOCK -> "Wake Lock"
            else -> permission.substringAfterLast('.')
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private suspend fun handleUiEvent(
        event: ReceiverUiEvent,
        snackbarHostState: SnackbarHostState
    ) {
        when (event) {
            is ReceiverUiEvent.ShowToast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }

            is ReceiverUiEvent.ShowError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
                // Also show in snackbar for better visibility
                snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = "Dismiss"
                )
            }

            is ReceiverUiEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel
                )
            }

            is ReceiverUiEvent.NavigateBack -> {
                finish()
            }

            is ReceiverUiEvent.UpdateNotification -> {
                // Update notification with current track info
                updatePlaybackNotification(event.title, event.artist)
            }

            is ReceiverUiEvent.RequestPermissions -> {
                requestPermissions()
            }

            is ReceiverUiEvent.VibrateDevice -> {
                vibrateDevice()
            }
        }
    }

    private fun updatePlaybackNotification(title: String, artist: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID) // Must exist in drawable
            .setContentTitle(title)
            .setContentText(artist)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_CHANNEL_ID.toInt(), notification)
    }


    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrateDevice() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                acquire(WAKE_LOCK_TIMEOUT_MS) // 10 minutes max
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
            wakeLock = null
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were granted while app was in background
        if (!hasRequiredPermissions()) {
            Toast.makeText(
                this,
                "Some permissions are missing. Features may be limited.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onPause() {
        super.onPause()
        // Optionally release wake lock when app goes to background
        // depending on your requirements
    }
}