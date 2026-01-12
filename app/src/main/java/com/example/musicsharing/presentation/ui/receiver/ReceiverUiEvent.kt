package com.example.musicsharing.presentation.ui.receiver

sealed class ReceiverUiEvent {
    data class ShowToast(val message: String) : ReceiverUiEvent()
    data class ShowError(val message: String) : ReceiverUiEvent()
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : ReceiverUiEvent()
    object NavigateBack : ReceiverUiEvent()
    data class UpdateNotification(val title: String, val artist: String) : ReceiverUiEvent()
    object RequestPermissions : ReceiverUiEvent()
    object VibrateDevice : ReceiverUiEvent()
}