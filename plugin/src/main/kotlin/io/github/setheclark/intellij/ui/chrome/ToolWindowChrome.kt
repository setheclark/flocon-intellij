package io.github.setheclark.intellij.ui.chrome

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.stringResource
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.rememberSelectableLazyListState
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/** Warning banner for ADB-not-found and server errors. Renders nothing when there is no warning. */
@Composable
fun ChromeWarningBanner(state: ToolWindowChromeState, modifier: Modifier = Modifier) {
    val message = when (val warning = state.warning) {
        ChromeWarning.AdbNotFound -> stringResource("banner.adbNotFound")
        is ChromeWarning.ServerError -> stringResource("banner.serverError", warning.message)
        null -> return
    }
    WarningBanner(message = message, modifier = modifier.fillMaxWidth())
}

/** Device and app picker. The selection is shared, so it is reflected on every tab. */
@OptIn(ExperimentalJewelApi::class, ExperimentalFoundationApi::class)
@Composable
fun DeviceAppPicker(
    devices: DevicesRenderModel,
    onIntent: (ToolWindowChromeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Workaround for JEWEL-1244: sync external selection changes into list state
    val listState = rememberSelectableLazyListState()
    LaunchedEffect(devices.selectedIndex) {
        val key = devices.devices.getOrNull(devices.selectedIndex)?.key
        listState.selectedKeys = if (key != null) setOf(key) else emptySet()
    }

    val selectedDevice = devices.devices.getOrNull(devices.selectedIndex)
    Tooltip(tooltip = { Text(selectedDevice?.fullLabel.orEmpty()) }) {
        ListComboBox(
            modifier = modifier.widthIn(max = 320.dp),
            items = devices.devices.map { it.shortLabel },
            selectedIndex = devices.selectedIndex,
            onSelectedItemChange = { index ->
                devices.devices.getOrNull(index)?.let { device ->
                    onIntent(ToolWindowChromeIntent.SelectDevice(device))
                }
            },
            listState = listState,
            itemKeys = { index, _ -> devices.devices.getOrNull(index)?.key.orEmpty() },
        )
    }
}

/** Start / stop / retry button reflecting the current server state. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerControlButton(
    serverState: MessageServerState,
    onIntent: (ToolWindowChromeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (serverIcon, serverEnabled, serverTooltipKey) = when (serverState) {
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
            when (serverState) {
                is MessageServerState.Running -> onIntent(ToolWindowChromeIntent.StopServer)

                is MessageServerState.Stopped,
                is MessageServerState.Error,
                -> onIntent(ToolWindowChromeIntent.StartServer)

                else -> Unit
            }
        },
        modifier = modifier,
        focusable = false,
        tooltip = { Text(stringResource(serverTooltipKey)) },
    )
}
