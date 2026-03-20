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
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.di.ProjectGraph
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.WarningBanner
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterPanel
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

    // Instantiate NetworkCallTabManager to keep its tab-management side effects alive.
    remember { uiGraph.networkCallTabManager }

    val warningMessage: String? = when {
        state.adbStatus == AdbStatus.NotFound ->
            stringResource("banner.adbNotFound")

        state.serverState is MessageServerState.Error ->
            stringResource(
                "banner.serverError",
                (state.serverState as MessageServerState.Error).message,
            )

        else -> null
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (warningMessage != null) {
            WarningBanner(
                message = warningMessage,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        NetworkFilterPanel(viewModel = uiGraph.networkFilterViewModel)
        Row(modifier = Modifier.fillMaxSize()) {
            NetworkInspectorToolbar(
                state = state,
                onIntent = { uiGraph.networkInspectorViewModel.dispatch(it) },
                modifier = Modifier.fillMaxHeight(),
            )
            NetworkCallList(
                listState = listState,
                selectedCallId = state.selectedCallId,
                sortAscending = state.sortAscending,
                onIntent = { uiGraph.networkCallListViewModel.dispatch(it) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}
