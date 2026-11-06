package io.github.setheclark.intellij.ui.network.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.stringResource
import io.github.setheclark.intellij.ui.chrome.DeviceAppPicker
import io.github.setheclark.intellij.ui.chrome.DevicesRenderModel
import io.github.setheclark.intellij.ui.chrome.ToolWindowChromeIntent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.painter.hints.Stateful
import org.jetbrains.jewel.ui.theme.textFieldStyle

@Composable
fun NetworkFilterPanel(
    viewModel: NetworkFilterViewModel,
    devices: DevicesRenderModel,
    onChromeIntent: (ToolWindowChromeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .distinctUntilChanged()
            .collectLatest { text ->
                viewModel.dispatch(NetworkFilterIntent.UpdateFilter(text))
            }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceAppPicker(devices = devices, onIntent = onChromeIntent)

        TextField(
            modifier = Modifier.weight(1f),
            state = searchState,
            placeholder = {
                Text(text = stringResource("label.filter.placeholder"))
            },
            leadingIcon = {
                Icon(
                    key = AllIconsKeys.Actions.Find,
                    contentDescription = null,
                )
            },
            trailingIcon = if (searchState.text.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { searchState.setTextAndPlaceCursorAtEnd("") },
                        style = JewelTheme.textFieldStyle.iconButtonStyle,
                    ) { buttonState ->
                        Icon(
                            key = AllIconsKeys.General.Close,
                            contentDescription = null,
                            hint = Stateful(buttonState),
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}
