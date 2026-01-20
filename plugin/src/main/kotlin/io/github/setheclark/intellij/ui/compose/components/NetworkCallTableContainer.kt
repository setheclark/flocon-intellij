package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.setheclark.intellij.ui.network.list.NetworkCallListIntent
import io.github.setheclark.intellij.ui.network.list.NetworkCallListState
import io.github.setheclark.intellij.ui.network.list.NetworkCallListViewModel

/**
 * Container that handles state collection for NetworkCallTable.
 *
 * This wrapper is necessary during Swing-Compose interop (Phase 3.2) to enable
 * proper reactive state updates within JewelComposePanel. The composable collects
 * state internally, allowing the UI to react to changes without panel recreation.
 *
 * @param viewModel The view model providing network call list state
 * @param selectedCallId Currently selected call ID (from parent inspector state)
 * @param modifier Optional modifier
 */
@Composable
fun NetworkCallTableContainer(
    viewModel: NetworkCallListViewModel,
    selectedCallId: String?,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState(
        initial = NetworkCallListState(
            calls = emptyList(),
            autoScrollEnabled = false
        )
    )

    NetworkCallTable(
        calls = state.calls,
        selectedCallId = selectedCallId,
        autoScrollEnabled = state.autoScrollEnabled,
        onSelectCall = { callId ->
            viewModel.dispatch(NetworkCallListIntent.SelectCall(callId))
        },
        onClearSelection = {
            viewModel.dispatch(NetworkCallListIntent.ClearCallSelection)
        },
        onDisableAutoScroll = {
            viewModel.dispatch(NetworkCallListIntent.DisableAutoScroll)
        },
        modifier = modifier
    )
}
