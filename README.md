# 🎵 Music Sharing over Local Wi-Fi (Android)

An Android application that allows **one device to broadcast audio** over a **local Wi-Fi network (no internet required)** and **multiple devices to discover and stream it in real time**.

The system uses:
- Foreground Service
- NanoHTTPD (HTTP audio streaming)
- UDP broadcast for server discovery
- MVVM architecture
- Kotlin Coroutines + StateFlow
- MediaPlayer-based receiver

---

## ✨ Features

### Sender (Broadcaster)
- Select an audio file from the device
- Start a foreground service
- Stream audio using a local HTTP server
- Broadcast server info (IP + port) via UDP
- Works completely offline

### Receiver (Listener)
- Discover available music servers automatically
- Select a server from the list
- Stream audio in real time
- Play / Pause / Seek controls
- Playback progress tracking
- Handles network buffering gracefully


