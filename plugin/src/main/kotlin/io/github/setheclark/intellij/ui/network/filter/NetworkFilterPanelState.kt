package io.github.setheclark.intellij.ui.network.filter

import io.github.setheclark.intellij.domain.models.RequestTypeFilter
import io.github.setheclark.intellij.domain.models.StatusGroup

data class NetworkFilterPanelState(
    val devices: DevicesRenderModel,
    val filterText: String,
    val filterExpanded: Boolean = false,
    val activeMethodFilters: Set<String> = emptySet(),
    val activeStatusFilters: Set<StatusGroup> = emptySet(),
    val activeTypeFilters: Set<RequestTypeFilter> = emptySet(),
)

data class DevicesRenderModel(
    val devices: List<DeviceFilterItem>,
    val selectedIndex: Int,
)

sealed interface NetworkFilterIntent {

    // Filtering
    data class UpdateFilter(val filter: String) : NetworkFilterIntent

    data class UpdateDeviceSelection(val device: DeviceFilterItem) : NetworkFilterIntent

    data class ToggleMethodFilter(val method: String) : NetworkFilterIntent

    data class ToggleStatusFilter(val group: StatusGroup) : NetworkFilterIntent

    data class ToggleTypeFilter(val type: RequestTypeFilter) : NetworkFilterIntent

    data object ToggleFilterPanel : NetworkFilterIntent
}
