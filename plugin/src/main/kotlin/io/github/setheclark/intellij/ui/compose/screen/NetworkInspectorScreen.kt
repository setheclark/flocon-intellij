package io.github.setheclark.intellij.ui.compose.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.ui.compose.components.NetworkDetailViewContainer
import io.github.setheclark.intellij.ui.compose.components.NetworkFilterBarContainer
import io.github.setheclark.intellij.ui.compose.components.NetworkCallTableContainer
import io.github.setheclark.intellij.ui.compose.components.NetworkToolbar
import io.github.setheclark.intellij.ui.compose.components.StatusBar
import io.github.setheclark.intellij.ui.compose.components.WarningBanner
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel
import io.github.setheclark.intellij.ui.network.detail.DetailPanelViewModel
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterViewModel
import io.github.setheclark.intellij.ui.network.list.NetworkCallListViewModel
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider

/**
 * Pure Compose network inspector screen.
 *
 * Final migration in Phase 5. Replaces NetworkInspectorPanel with pure Compose UI.
 * Layout: Toolbar, Filter Bar, Warning Banner, Split View (List + Detail), Status Bar.
 *
 * @param project IntelliJ project for editor integration
 * @param viewModel Main network inspector view model
 * @param filterViewModel Filter bar view model
 * @param listViewModel Network call list view model
 * @param detailViewModel Detail panel view model
 * @param modifier Optional modifier
 */
@Composable
fun NetworkInspectorScreen(
    project: Project,
    viewModel: NetworkInspectorViewModel,
    filterViewModel: NetworkFilterViewModel,
    listViewModel: NetworkCallListViewModel,
    detailViewModel: DetailPanelViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar with action buttons
        NetworkToolbar(
            autoScrollEnabled = state.autoScrollEnabled,
            serverState = state.serverState,
            onAction = viewModel::dispatch,
            modifier = Modifier.fillMaxWidth()
        )

        // Filter bar (search and device selector)
        NetworkFilterBarContainer(
            viewModel = filterViewModel,
            modifier = Modifier.fillMaxWidth()
        )

        // Warning banner (ADB not found or server error)
        val warningText = when {
            state.adbStatus == AdbStatus.NotFound -> {
                "ADB not found. USB device connections won't work. " +
                        "Add 'adb' to PATH or set ANDROID_HOME environment variable."
            }
            state.serverState is MessageServerState.Error -> {
                val message = (state.serverState as MessageServerState.Error).message
                "ERROR: '$message' - You may have the Flocon desktop app running. " +
                        "If so, close the app and click the server retry action above."
            }
            else -> ""
        }

        val isWarningVisible = state.adbStatus == AdbStatus.NotFound || state.serverState is MessageServerState.Error

        WarningBanner(
            text = warningText,
            isVisible = isWarningVisible,
            modifier = Modifier.fillMaxWidth()
        )

        // Main content: list on left, optional detail on right
        if (state.isDetailVisible) {
            // Both panels visible side-by-side (40% list, 60% detail)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Network calls list
                NetworkCallTableContainer(
                    viewModel = listViewModel,
                    selectedCallId = state.selectedCallId,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )

                // Divider
                Divider(
                    orientation = Orientation.Vertical,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )

                // Detail panel
                NetworkDetailViewContainer(
                    project = project,
                    viewModel = detailViewModel,
                    onClose = {
                        listViewModel.dispatch(io.github.setheclark.intellij.ui.network.list.NetworkCallListIntent.ClearCallSelection)
                    },
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Only list visible (full width)
            NetworkCallTableContainer(
                viewModel = listViewModel,
                selectedCallId = state.selectedCallId,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // Status bar at bottom
        StatusBar(
            serverState = state.serverState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
