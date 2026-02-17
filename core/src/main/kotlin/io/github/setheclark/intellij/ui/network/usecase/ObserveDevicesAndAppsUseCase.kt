package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.models.DeviceId
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.device.usecase.ObserveDevicesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Inject
class ObserveDevicesAndAppsUseCase(
    private val observeDevicesUseCase: ObserveDevicesUseCase,
    private val devicesRepository: DevicesRepository,
) {
    operator fun invoke(): Flow<List<DeviceAndAppModel>> {
        return observeDevicesUseCase().flatMapLatest { devices ->
            if (devices.isEmpty()) {
                flowOf(emptyList())
            } else {
                val appFlows = devices.map { device ->
                    devicesRepository.observeDeviceApps(device.deviceId)
                        .map { apps ->
                            apps.map { app ->
                                DeviceAndAppModel(
                                    deviceName = device.deviceName,
                                    deviceId = device.deviceId,
                                    packageName = app.packageName,
                                    appInstance = app.lastAppInstance.toString(),
                                )
                            }
                        }
                }
                combine(appFlows) { appLists -> appLists.flatMap { it } }
            }
        }
    }
}

data class DeviceAndAppModel(
    val deviceName: String,
    val deviceId: DeviceId,
    val packageName: String,
    val appInstance: String,
)