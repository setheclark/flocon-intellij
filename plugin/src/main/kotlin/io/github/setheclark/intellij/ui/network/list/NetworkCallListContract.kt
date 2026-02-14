package io.github.setheclark.intellij.ui.network.list


data class NetworkCallListState(
    val calls: List<NetworkCallListItem>,
    val autoScrollEnabled: Boolean,
)

sealed interface NetworkCallListIntent {
    data object DisableAutoScroll : NetworkCallListIntent
    data object ClearCallSelection : NetworkCallListIntent
    data class SelectCall(val callId: String) : NetworkCallListIntent
    data class OpenCallInTab(val callId: String, val callName: String) : NetworkCallListIntent
}