package io.github.setheclark.intellij.ui.network.filter

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdAndPackageNameUseCase
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.ui.network.NetworkInspectorIntent.UpdateFilter
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel
import io.github.setheclark.intellij.ui.network.usecase.ObserveDevicesAndAppsUseCase
import io.github.setheclark.intellij.ui.network.usecase.SelectDeviceAndAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Inject
class NetworkFilterViewModel(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val parentViewModel: NetworkInspectorViewModel,
    private val selectDeviceAndAppUseCase: SelectDeviceAndAppUseCase,
    private val observeDevicesAndAppsUseCase: ObserveDevicesAndAppsUseCase,
    private val observeCurrentDeviceIdAndPackageNameUseCase: ObserveCurrentDeviceIdAndPackageNameUseCase,
) {

    private val filterFlow = parentViewModel.state.map { it.filter }.distinctUntilChanged()

    private val devices = combine(
        observeDevicesAndAppsUseCase(),
        observeCurrentDeviceIdAndPackageNameUseCase().filterNotNull(),
    ) { devices, current ->
        // Deduplicate by deviceId + packageName
        val uniqueDevices = devices.distinctBy { it.deviceId to it.packageName }

        var selectedIndex = -1
        val items = uniqueDevices.mapIndexed { index, device ->
            if (current.deviceId == device.deviceId && device.packageName == current.packageName) {
                selectedIndex = index
            }

            DeviceFilterItem(
                displayName = device.deviceName.takeUnless { it.isEmpty() } ?: device.deviceId.take(12),
                packageName = device.packageName,
                deviceId = device.deviceId,
            )
        }

        DevicesRenderModel(
            devices = items,
            selectedIndex = selectedIndex,
        )
    }

    val state = combine(
        filterFlow,
        devices,
    ) { filter, devices ->
        NetworkFilterPanelState(
            devices = devices,
            filterText = filter.searchText,
        )
    }

    fun dispatch(intent: NetworkFilterIntent) {
        when (intent) {
            is NetworkFilterIntent.UpdateFilter -> {
                parentViewModel.dispatch(UpdateFilter(intent.filter))
            }

            is NetworkFilterIntent.UpdateDeviceSelection -> {
                scope.launch {
                    selectDeviceAndAppUseCase(intent.device.deviceId, intent.device.packageName)
                }
            }
        }
    }
}
