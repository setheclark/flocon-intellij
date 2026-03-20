package io.github.setheclark.intellij.ui.network

import androidx.compose.runtime.Immutable
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.domain.models.RequestTypeFilter
import io.github.setheclark.intellij.domain.models.StatusGroup
import io.github.setheclark.intellij.server.MessageServerState

@Immutable
data class NetworkInspectorState(
    val selectedCallId: String? = null,
    val filter: NetworkFilter = NetworkFilter(),
    val serverState: MessageServerState = MessageServerState.Stopped,
    val adbStatus: AdbStatus = AdbStatus.Initializing,
    val autoScrollEnabled: Boolean = true,
    val filterExpanded: Boolean = false,
    val activeMethodFilters: Set<String> = emptySet(),
    val activeStatusFilters: Set<StatusGroup> = emptySet(),
    val activeTypeFilters: Set<RequestTypeFilter> = emptySet(),
    val sortAscending: Boolean = true,
)

sealed interface NetworkInspectorIntent {
    // Selection
    data class SelectCall(val callId: String?) : NetworkInspectorIntent

    // Filtering
    data class UpdateFilter(val filter: String) : NetworkInspectorIntent
    data class ToggleMethodFilter(val method: String) : NetworkInspectorIntent
    data class ToggleStatusFilter(val group: StatusGroup) : NetworkInspectorIntent
    data class ToggleTypeFilter(val type: RequestTypeFilter) : NetworkInspectorIntent
    data object ToggleFilterPanel : NetworkInspectorIntent

    // List actions
    data object ClearAll : NetworkInspectorIntent
    data object EnableAutoScroll : NetworkInspectorIntent
    data object DisableAutoScroll : NetworkInspectorIntent
    data object ToggleSortOrder : NetworkInspectorIntent

    // Server control
    data object StartServer : NetworkInspectorIntent
    data object StopServer : NetworkInspectorIntent

    // Tab management
    data class OpenCallInTab(val callId: String, val callName: String) : NetworkInspectorIntent
}

data class OpenCallInTabEvent(val callId: String, val callName: String)
