package io.github.setheclark.intellij.ui.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.setheclark.intellij.di.ProjectGraph
import io.github.setheclark.intellij.ui.chrome.ChromeWarningBanner
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterPanel
import io.github.setheclark.intellij.ui.network.list.ColumnConfigDialog
import io.github.setheclark.intellij.ui.network.list.NetworkCallList

/**
 * Root Compose content for the Network Inspector tool window.
 */
@Composable
fun NetworkInspectorContent(
    uiGraph: ProjectGraph,
    modifier: Modifier = Modifier,
) {
    val state by uiGraph.networkInspectorViewModel.state.collectAsState()
    val listState by uiGraph.networkCallListViewModel.state.collectAsState()
    val chromeState by uiGraph.toolWindowChromeViewModel.state.collectAsState()

    // Instantiate NetworkCallTabManager to keep its tab-management side effects alive.
    remember { uiGraph.networkCallTabManager }

    Column(modifier = modifier.fillMaxSize()) {
        ChromeWarningBanner(state = chromeState, modifier = Modifier.fillMaxWidth())
        NetworkFilterPanel(
            viewModel = uiGraph.networkFilterViewModel,
            devices = chromeState.devices,
            onChromeIntent = { uiGraph.toolWindowChromeViewModel.dispatch(it) },
        )
        Row(modifier = Modifier.fillMaxSize()) {
            NetworkInspectorToolbar(
                state = state,
                serverState = chromeState.serverState,
                onIntent = { uiGraph.networkInspectorViewModel.dispatch(it) },
                onChromeIntent = { uiGraph.toolWindowChromeViewModel.dispatch(it) },
                onConfigureColumns = {
                    val dialog = ColumnConfigDialog(uiGraph.project, uiGraph.settingsProvider)
                    dialog.show()
                },
                modifier = Modifier.fillMaxHeight(),
            )
            NetworkCallList(
                listState = listState,
                selectedCallId = state.selectedCallId,
                visibleColumns = state.visibleColumns,
                columnWidths = state.columnWidths,
                onColumnWidthChange = { widths ->
                    uiGraph.networkInspectorViewModel.dispatch(
                        NetworkInspectorIntent.UpdateColumnWidths(widths),
                    )
                },
                onIntent = { uiGraph.networkCallListViewModel.dispatch(it) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}
