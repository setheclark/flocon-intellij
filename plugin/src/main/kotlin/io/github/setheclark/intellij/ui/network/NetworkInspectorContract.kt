package io.github.setheclark.intellij.ui.network

import androidx.compose.runtime.Immutable
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.ui.network.list.NetworkCallListColumn

@Immutable
data class NetworkInspectorState(
    val selectedCallId: String? = null,
    val filter: NetworkFilter = NetworkFilter(),
    val autoScrollEnabled: Boolean = true,
    val visibleColumns: List<NetworkCallListColumn> = NetworkCallListColumn.entries,
    val columnWidths: Map<String, Float> = emptyMap(),
)

sealed interface NetworkInspectorIntent {
    // Selection
    data class SelectCall(val callId: String?) : NetworkInspectorIntent

    // Filtering
    data class UpdateFilter(val filter: String) : NetworkInspectorIntent

    // List actions
    data object ClearHistory : NetworkInspectorIntent
    data object EnableAutoScroll : NetworkInspectorIntent
    data object DisableAutoScroll : NetworkInspectorIntent

    data class UpdateColumnWidths(val widths: Map<String, Float>) : NetworkInspectorIntent

    // Tab management
    data class OpenCallInTab(val callId: String, val callName: String) : NetworkInspectorIntent
}

data class OpenCallInTabEvent(val callId: String, val callName: String)
