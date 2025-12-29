package io.github.setheclark.intellij.ui.network.list


data class NetworkCallListState(
    val calls: List<NetworkCallListItem>,
    val autoScrollEnabled: Boolean,
)

sealed interface NetworkCallListIntent {
    object DisableAutoScroll : NetworkCallListIntent
    object ClearCallSelection : NetworkCallListIntent
    data class SelectCall(val callId: String) : NetworkCallListIntent
}