package io.github.setheclark.intellij.ui.network

import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.server.MessageServerState

data class NetworkInspectorState(
    val selectedCallId: String? = null,
    val filter: NetworkFilter = NetworkFilter(),
    val serverState: MessageServerState = MessageServerState.Stopped,
    val adbStatus: AdbStatus = AdbStatus.Initializing,
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
