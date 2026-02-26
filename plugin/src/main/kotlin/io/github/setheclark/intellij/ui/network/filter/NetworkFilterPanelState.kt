package io.github.setheclark.intellij.ui.network.filter

data class NetworkFilterPanelState(
    val devices: DevicesRenderModel,
    val filterText: String,
)

data class DevicesRenderModel(
    val devices: List<DeviceFilterItem>,
    val selectedIndex: Int,
)

sealed interface NetworkFilterIntent {

    // Filtering
    data class UpdateFilter(val filter: String) : NetworkFilterIntent

    data class UpdateDeviceSelection(val device: DeviceFilterItem) : NetworkFilterIntent
}
