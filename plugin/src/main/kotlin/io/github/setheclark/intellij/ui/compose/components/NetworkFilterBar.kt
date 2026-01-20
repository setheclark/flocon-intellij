package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.setheclark.intellij.ui.network.filter.DeviceFilterItem
import io.github.setheclark.intellij.ui.network.filter.DevicesRenderModel
import io.github.setheclark.intellij.ui.network.filter.NetworkFilterIntent

/**
 * Compose version of NetworkFilterPanel.
 * Provides search and device filtering functionality.
 *
 * @param filterText Current search filter text
 * @param devicesModel Device selection model
 * @param onIntent Callback to dispatch filter intents
 * @param modifier Optional modifier for the bar
 */
@Composable
fun NetworkFilterBar(
    filterText: String,
    devicesModel: DevicesRenderModel,
    onIntent: (NetworkFilterIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchField(
            value = filterText,
            onValueChange = { newText ->
                onIntent(NetworkFilterIntent.UpdateFilter(newText.trim()))
            },
            placeholder = "Filter requests..."
        )

        DeviceSelector(
            devices = devicesModel.devices,
            selectedIndex = devicesModel.selectedIndex,
            onDeviceSelected = { device ->
                onIntent(NetworkFilterIntent.UpdateDeviceSelection(device))
            }
        )
    }
}
