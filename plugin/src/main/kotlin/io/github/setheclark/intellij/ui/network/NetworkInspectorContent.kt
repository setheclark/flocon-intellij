package io.github.setheclark.intellij.ui.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.intellij.openapi.application.ApplicationManager
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.di.ProjectGraph
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.WarningBanner
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterPanel
import io.github.setheclark.intellij.ui.network.list.ColumnConfigDialog
import io.github.setheclark.intellij.ui.network.list.NetworkCallList
import io.github.setheclark.intellij.ui.network.list.NetworkCallListState

/**
 * Root Compose content for the Network Inspector tool window.
 */
@Composable
fun NetworkInspectorContent(
    uiGraph: ProjectGraph,
    modifier: Modifier = Modifier,
) {
    val state by uiGraph.networkInspectorViewModel.state.collectAsState()

    // Cache unscoped DI instances so they survive recomposition.
    val networkCallListViewModel = remember { uiGraph.networkCallListViewModel }
    val listState by networkCallListViewModel.state.collectAsState()

    // Instantiate NetworkCallTabManager to keep its tab-management side effects alive.
    remember { uiGraph.networkCallTabManager }

    var columnsVersion by remember { mutableIntStateOf(0) }

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
                onConfigureColumns = {
                    ApplicationManager.getApplication().invokeLater {
                        val dialog = ColumnConfigDialog(uiGraph.project)
                        if (dialog.showAndGet()) {
                            columnsVersion++
                        }
                    }
                },
                modifier = Modifier.fillMaxHeight(),
            )
            NetworkCallList(
                listState = listState,
                selectedCallId = state.selectedCallId,
                columnsVersion = columnsVersion,
                onIntent = { networkCallListViewModel.dispatch(it) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}
