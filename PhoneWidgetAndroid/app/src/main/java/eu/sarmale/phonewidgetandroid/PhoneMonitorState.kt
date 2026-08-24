package eu.sarmale.phonewidgetandroid

import kotlinx.coroutines.flow.MutableStateFlow

object PhoneMonitorState {
    val lastMessage = MutableStateFlow("Waiting for a message from the watch...")
    val isServiceRunning = MutableStateFlow(false)
}