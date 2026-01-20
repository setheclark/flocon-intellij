package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterViewModel

/**
 * Container that handles state collection for NetworkFilterBar.
 *
 * This wrapper is necessary during Swing-Compose interop (Phases 1-4) to enable
 * proper reactive state updates within JewelComposePanel. The composable collects
 * state internally, allowing the UI to react to changes without panel recreation.
 *
 * Will be removed in Phase 5 when migrating to pure Compose.
 */
@Composable
fun NetworkFilterBarContainer(
    viewModel: NetworkFilterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState(initial = io.github.setheclark.intellij.ui.network.filter.NetworkFilterPanelState(
        filterText = "",
        devices = io.github.setheclark.intellij.ui.network.filter.DevicesRenderModel(emptyList(), -1)
    ))

    NetworkFilterBar(
        filterText = state.filterText,
        devicesModel = state.devices,
        onIntent = { intent -> viewModel.dispatch(intent) },
        modifier = modifier
    )
}
