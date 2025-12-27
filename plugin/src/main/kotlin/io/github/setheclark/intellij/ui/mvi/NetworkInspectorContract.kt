package io.github.setheclark.intellij.ui.mvi

import io.github.setheclark.intellij.domain.models.*
import io.github.setheclark.intellij.ui.list.applyFilter

/**
 * Immutable UI state for the Network Inspector.
 * Single source of truth for all UI components.
 */
data class NetworkInspectorState(
    val calls: List<NetworkCallEntry> = emptyList(),
    val selectedCall: NetworkCallEntry? = null,
    val filter: NetworkFilter = NetworkFilter(),
    val serverState: ServerState = ServerState.Stopped,
    val adbStatus: AdbStatus = AdbStatus.Initializing,
    val connectedDevices: Set<ConnectedDevice> = emptySet(),
    val autoScrollEnabled: Boolean = true,
) {
    /**
     * Derived property: filtered calls based on current filter.
     * Computed on access to avoid redundant filtering.
     */
    val filteredCalls: List<NetworkCallEntry>
        get() = calls.applyFilter(filter)

    /**
     * Whether the detail panel should be visible.
     */
    val isDetailVisible: Boolean
        get() = selectedCall != null
}

/**
 * User intents (actions) for the Network Inspector.
 * Represents all possible user interactions.
 */
sealed interface NetworkInspectorIntent {
    // Selection
    data class SelectCall(val call: NetworkCallEntry?) : NetworkInspectorIntent

    // Filtering
    data class UpdateFilter(val filter: NetworkFilter) : NetworkInspectorIntent

    // List actions
    data object ClearAll : NetworkInspectorIntent
    data object EnableAutoScroll : NetworkInspectorIntent
    data object DisableAutoScroll : NetworkInspectorIntent

    // Server control
    data object StartServer : NetworkInspectorIntent
    data object StopServer : NetworkInspectorIntent
}
