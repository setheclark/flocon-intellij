package io.github.setheclark.intellij.ui.chrome

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdAndPackageNameUseCase
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.setheclark.intellij.adb.AdbStatusDataSource
import io.github.setheclark.intellij.di.ProjectScope
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.server.usecase.StopMessageServerUseCase
import io.github.setheclark.intellij.ui.chrome.usecase.ObserveDevicesAndAppsUseCase
import io.github.setheclark.intellij.ui.chrome.usecase.ObserveServerStatusUseCase
import io.github.setheclark.intellij.ui.chrome.usecase.SelectDeviceAndAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the device/app picker, server controls and warning banner shown on every feature tab.
 *
 * The selection lives in the shared devices repository, so a selection made on one tab is seen by all.
 */
@Inject
@SingleIn(ProjectScope::class)
class ToolWindowChromeViewModel(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val adbStatusDataSource: AdbStatusDataSource,
    private val observeServerStatusUseCase: ObserveServerStatusUseCase,
    private val startServerUseCase: StartServerUseCase,
    private val stopServerUseCase: StopMessageServerUseCase,
    private val selectDeviceAndAppUseCase: SelectDeviceAndAppUseCase,
    observeDevicesAndAppsUseCase: ObserveDevicesAndAppsUseCase,
    observeCurrentDeviceIdAndPackageNameUseCase: ObserveCurrentDeviceIdAndPackageNameUseCase,
) {

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
                appName = device.appName,
                deviceId = device.deviceId,
            )
        }

        DevicesRenderModel(
            devices = items,
            selectedIndex = selectedIndex,
        )
    }.onStart {
        // Server status and the warning banner must not wait for a device to be selected.
        emit(DevicesRenderModel(emptyList(), -1))
    }

    val state: StateFlow<ToolWindowChromeState> = combine(
        devices,
        observeServerStatusUseCase(),
        adbStatusDataSource.status,
    ) { devices, serverState, adbStatus ->
        ToolWindowChromeState(devices = devices, serverState = serverState, adbStatus = adbStatus)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ToolWindowChromeState(
            serverState = observeServerStatusUseCase().value,
            adbStatus = adbStatusDataSource.status.value,
        ),
    )

    fun dispatch(intent: ToolWindowChromeIntent) {
        when (intent) {
            is ToolWindowChromeIntent.SelectDevice -> scope.launch {
                selectDeviceAndAppUseCase(intent.device.deviceId, intent.device.packageName)
            }

            ToolWindowChromeIntent.StartServer -> scope.launch { startServerUseCase() }

            ToolWindowChromeIntent.StopServer -> scope.launch { stopServerUseCase() }
        }
    }
}
