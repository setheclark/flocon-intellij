package io.github.setheclark.intellij.flocon.device

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.device.datasource.local.LocalDevicesDataSource
import io.github.openflocon.data.core.device.datasource.local.model.InsertResult
import io.github.openflocon.domain.device.models.AppPackageName
import io.github.openflocon.domain.device.models.DeviceAppDomainModel
import io.github.openflocon.domain.device.models.DeviceDomainModel
import io.github.openflocon.domain.device.models.DeviceId
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Inject
@SingleIn(AppScope::class)
class InMemoryLocalDevicesDataSource : LocalDevicesDataSource {

    private val log = Logger.withPluginTag("LocalDevicesDataSource")

    // In-memory storage using StateFlow for reactivity
    private val devicesState = MutableStateFlow<Map<DeviceId, DeviceDomainModel>>(emptyMap())
    private val appsState = MutableStateFlow<Map<DeviceId, Map<AppPackageName, DeviceAppDomainModel>>>(emptyMap())

    // region device

    override val devices: Flow<List<DeviceDomainModel>>
        get() = devicesState.map { it.values.toList() }

    override fun observeDeviceById(it: DeviceId): Flow<DeviceDomainModel?> {
        return devicesState.map { devices -> devices[it] }
    }

    override suspend fun getDeviceById(it: DeviceId): DeviceDomainModel? {
        return devicesState.value[it]
    }

    override suspend fun insertDevice(device: DeviceDomainModel): InsertResult {
        log.d { "insertDevice: deviceId=${device.deviceId}, deviceName=${device.deviceName}" }
        val exists = devicesState.value.containsKey(device.deviceId)
        log.d { "insertDevice: exists=$exists, currentDevices=${devicesState.value.keys}" }
        return if (exists) {
            InsertResult.Exists
        } else {
            devicesState.update { current ->
                current + (device.deviceId to device)
            }
            log.d { "insertDevice: added new device, total devices=${devicesState.value.size}" }
            InsertResult.New
        }
    }

    // endregion

    // region apps

    override suspend fun insertDeviceApp(
        deviceId: DeviceId,
        app: DeviceAppDomainModel
    ): InsertResult {
        val deviceApps = appsState.value[deviceId] ?: emptyMap()
        val exists = deviceApps.containsKey(app.packageName)
        return if (exists) {
            InsertResult.Exists
        } else {
            appsState.update { current ->
                val updatedDeviceApps = (current[deviceId] ?: emptyMap()) + (app.packageName to app)
                current + (deviceId to updatedDeviceApps)
            }
            InsertResult.New
        }
    }

    override fun observeDeviceApps(deviceId: DeviceId): Flow<List<DeviceAppDomainModel>> {
        return appsState.map { apps ->
            apps[deviceId]?.values?.toList() ?: emptyList()
        }
    }

    override suspend fun getDeviceAppByPackage(
        deviceId: DeviceId,
        packageName: AppPackageName
    ): DeviceAppDomainModel? {
        return appsState.value[deviceId]?.get(packageName)
    }

    override fun observeDeviceAppByPackage(
        deviceId: DeviceId,
        packageName: AppPackageName
    ): Flow<DeviceAppDomainModel?> {
        return appsState.map { apps ->
            apps[deviceId]?.get(packageName)
        }
    }

    // endregion

    // region no-op

    override suspend fun saveAppIcon(
        deviceId: DeviceId,
        appPackageName: AppPackageName,
        iconEncoded: String
    ) {
        log.w { "no-op:saveAppIcon: $deviceId|$appPackageName" }
    }

    override suspend fun hasAppIcon(
        deviceId: DeviceId,
        appPackageName: AppPackageName
    ): Boolean {
        log.w { "no-op:hasAppIcon: $deviceId|$appPackageName" }
        return false
    }

    override fun observeDeviceSdkVersion(
        deviceId: DeviceId,
        appPackageName: String
    ): Flow<String?> {
        log.w { "no-op:observeDeviceSdkVersion: $deviceId|$appPackageName" }
        return flowOf(null)
    }

    override suspend fun delete(deviceId: DeviceId) {
        devicesState.update { current ->
            current - deviceId
        }
        appsState.update { current ->
            current - deviceId
        }
    }

    override suspend fun deleteApp(
        deviceId: DeviceId,
        packageName: AppPackageName
    ) {
        appsState.update { current ->
            val deviceApps = current[deviceId] ?: return@update current
            val updatedDeviceApps = deviceApps - packageName
            if (updatedDeviceApps.isEmpty()) {
                current - deviceId
            } else {
                current + (deviceId to updatedDeviceApps)
            }
        }
    }

    override suspend fun clear() {
        devicesState.value = emptyMap()
        appsState.value = emptyMap()
    }

    // endregion
}