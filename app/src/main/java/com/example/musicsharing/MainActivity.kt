package com.example.musicsharing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.musicsharing.ui.receiver.ReceiverActivity
import com.example.musicsharing.ui.screens.MainScreen
import com.example.musicsharing.ui.sender.SenderActivity
import com.example.musicsharing.ui.theme.MusicSharingTheme
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicSharingTheme {
                MainScreen(
                    onSenderClick = {
                        startActivity(
                            Intent(this, SenderActivity::class.java)
                        )
                    },
                    onReceiverClick = {
                        startActivity(
                            Intent(this, ReceiverActivity::class.java)
                        )
                    }
                )
            }
        }
    }
}