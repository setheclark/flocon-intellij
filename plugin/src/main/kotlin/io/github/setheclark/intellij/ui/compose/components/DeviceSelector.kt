package io.github.setheclark.intellij.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.setheclark.intellij.ui.network.filter.DeviceFilterItem
import org.jetbrains.jewel.ui.component.Dropdown

/**
 * A dropdown selector for filtering by device.
 *
 * @param devices List of available devices
 * @param selectedIndex Index of currently selected device
 * @param onDeviceSelected Callback when a device is selected
 * @param modifier Optional modifier for the dropdown
 */
@Composable
fun DeviceSelector(
    devices: List<DeviceFilterItem>,
    selectedIndex: Int,
    onDeviceSelected: (DeviceFilterItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (devices.isEmpty()) {
        return
    }

    val selectedItem = devices.getOrNull(selectedIndex) ?: devices.first()

    Dropdown(
        menuContent = {
            devices.forEach { device ->
                selectableItem(
                    selected = device == selectedItem,
                    onClick = { onDeviceSelected(device) }
                ) {
                    org.jetbrains.jewel.ui.component.Text(device.toString())
                }
            }
        },
        modifier = modifier
    ) {
        org.jetbrains.jewel.ui.component.Text(selectedItem.toString())
    }
}
