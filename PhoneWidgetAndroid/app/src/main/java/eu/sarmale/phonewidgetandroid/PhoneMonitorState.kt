package eu.sarmale.phonewidgetandroid

import kotlinx.coroutines.flow.MutableStateFlow

// Shared in-memory state so the Service (which owns the ConnectIQ connection)
// and the Activity/UI (which just displays things) can talk to each other
// without needing a Binder or broadcasts - they're in the same process.
object PhoneMonitorState {
    val lastMessage = MutableStateFlow("Waiting for a message from the watch...")
    val isServiceRunning = MutableStateFlow(false)
}