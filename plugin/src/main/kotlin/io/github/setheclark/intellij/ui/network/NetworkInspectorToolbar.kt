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
import io.github.setheclark.intellij.ui.toolwindow.ServerControlButton
import io.github.setheclark.intellij.ui.toolwindow.ToolWindowIntent
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
    serverState: MessageServerState,
    onIntent: (NetworkInspectorIntent) -> Unit,
    onToolWindowIntent: (ToolWindowIntent) -> Unit,
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

        ServerControlButton(serverState = serverState, onIntent = onToolWindowIntent)
    }
}
