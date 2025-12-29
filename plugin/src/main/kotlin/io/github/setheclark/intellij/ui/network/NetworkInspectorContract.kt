package io.github.setheclark.intellij.ui.network

import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.domain.models.ConnectedDevice
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.network.ServerState

data class NetworkInspectorState(
    val calls: List<NetworkCallEntry> = emptyList(),
    val selectedCallId: String? = null,
    val filter: NetworkFilter = NetworkFilter(),
    val serverState: ServerState = ServerState.Stopped,
    val adbStatus: AdbStatus = AdbStatus.Initializing,
    val connectedDevices: Set<ConnectedDevice> = emptySet(),
    val autoScrollEnabled: Boolean = true,
) {
    val isDetailVisible: Boolean
        get() = selectedCallId != null
}

sealed interface NetworkInspectorIntent {
    // Selection
    data class SelectCall(val callId: String?) : NetworkInspectorIntent

    // Filtering
    data class UpdateFilter(val filter: String) : NetworkInspectorIntent

    // List actions
    data object ClearAll : NetworkInspectorIntent
    data object EnableAutoScroll : NetworkInspectorIntent
    data object DisableAutoScroll : NetworkInspectorIntent

    // Server control
    data object StartServer : NetworkInspectorIntent
    data object StopServer : NetworkInspectorIntent
}
