package io.github.setheclark.intellij.ui.network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.stringResource
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.SelectableIconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkInspectorToolbar(
    state: NetworkInspectorState,
    onIntent: (NetworkInspectorIntent) -> Unit,
    onConfigureColumns: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().width(IntrinsicSize.Min),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconActionButton(
            key = AllIconsKeys.Actions.GC,
            contentDescription = stringResource("action.clearAll.text"),
            onClick = { onIntent(NetworkInspectorIntent.ClearAll) },
            focusable = false,
            tooltip = { Text(stringResource("action.clearAll.description")) },
        )

        Divider(
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        )

        SelectableIconActionButton(
            key = AllIconsKeys.RunConfigurations.Scroll_down,
            contentDescription = stringResource("action.autoScroll.text"),
            selected = state.autoScrollEnabled,
            onClick = {
                if (state.autoScrollEnabled) {
                    onIntent(NetworkInspectorIntent.DisableAutoScroll)
                } else {
                    onIntent(NetworkInspectorIntent.EnableAutoScroll)
                }
            },
            focusable = false,
            tooltip = { Text(stringResource("action.autoScroll.description")) },
        )

        Divider(
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        )

        IconActionButton(
            key = AllIconsKeys.General.Settings,
            contentDescription = stringResource("action.configureColumns.text"),
            onClick = onConfigureColumns,
            focusable = false,
            tooltip = { Text(stringResource("action.configureColumns.description")) },
        )

        Spacer(modifier = Modifier.weight(1f))

        val (serverIcon, serverEnabled, serverTooltipKey) = when (state.serverState) {
            is MessageServerState.Running ->
                Triple(AllIconsKeys.Actions.Suspend, true, "action.stopServer.description")

            is MessageServerState.Stopped ->
                Triple(AllIconsKeys.Actions.Execute, true, "action.startServer.description")

            is MessageServerState.Error ->
                Triple(AllIconsKeys.Actions.Restart, true, "action.retryServer.description")

            is MessageServerState.Starting ->
                Triple(AllIconsKeys.Actions.Execute, false, "action.startServer.description")

            is MessageServerState.Stopping ->
                Triple(AllIconsKeys.Actions.Suspend, false, "action.stopServer.description")
        }

        IconActionButton(
            key = serverIcon,
            contentDescription = stringResource(serverTooltipKey),
            enabled = serverEnabled,
            onClick = {
                when (state.serverState) {
                    is MessageServerState.Running -> onIntent(NetworkInspectorIntent.StopServer)

                    is MessageServerState.Stopped,
                    is MessageServerState.Error,
                    -> onIntent(NetworkInspectorIntent.StartServer)

                    else -> Unit
                }
            },
            focusable = false,
            tooltip = { Text(stringResource(serverTooltipKey)) },
        )
    }
}
