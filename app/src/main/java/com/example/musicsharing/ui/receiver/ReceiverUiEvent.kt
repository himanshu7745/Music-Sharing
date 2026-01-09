package com.example.musicsharing.ui.receiver

sealed class ReceiverUiEvent {
    data class ShowToast(val message: String) : ReceiverUiEvent()
    data class ShowError(val message: String) : ReceiverUiEvent()
}