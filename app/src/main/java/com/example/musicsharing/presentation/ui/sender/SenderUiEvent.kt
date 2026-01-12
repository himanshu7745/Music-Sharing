package com.example.musicsharing.presentation.ui.sender

sealed class SenderUiEvent {
    data class ShowToast(val message: String) : SenderUiEvent()
    data class Error(val message: String) : SenderUiEvent()
}