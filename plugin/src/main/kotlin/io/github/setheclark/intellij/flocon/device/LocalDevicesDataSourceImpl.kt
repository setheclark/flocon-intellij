package io.github.setheclark.intellij.flocon.device

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.device.datasource.local.LocalDevicesDataSource
import io.github.openflocon.data.core.device.datasource.local.model.InsertResult
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.models.AppPackageName
import io.github.openflocon.domain.device.models.DeviceAppDomainModel
import io.github.openflocon.domain.device.models.DeviceDomainModel
import io.github.openflocon.domain.device.models.DeviceId
import io.github.setheclark.intellij.DeviceAppEntity
import io.github.setheclark.intellij.DeviceAppEntityQueries
import io.github.setheclark.intellij.DeviceEntity
import io.github.setheclark.intellij.DeviceEntityQueries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
class LocalDevicesDataSourceImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val deviceEntityQueries: DeviceEntityQueries,
    private val deviceAppEntityQueries: DeviceAppEntityQueries,
) : LocalDevicesDataSource {

    private val log = Logger.withTag("LocalDevicesDataSource")

    override val devices: Flow<List<DeviceDomainModel>>
        get() = deviceEntityQueries.selectAll()
            .asFlow()
            .mapToList(dispatcherProvider.data)
            .map { items ->
                items.map { it.toModel() }
            }

    override fun observeDeviceById(it: DeviceId): Flow<DeviceDomainModel?> {
        return deviceEntityQueries.forDeviceId(it)
            .asFlow()
            .mapToOneOrNull(dispatcherProvider.data)
            .map { it?.toModel() }
    }

    override suspend fun getDeviceById(it: DeviceId): DeviceDomainModel? = withContext(dispatcherProvider.data) {
        deviceEntityQueries.forDeviceId(it)
            .executeAsOneOrNull()
            ?.toModel()
    }

    override suspend fun insertDevice(device: DeviceDomainModel): InsertResult {
        val entity = withContext(dispatcherProvider.data) {
            deviceEntityQueries.forDeviceId(device.deviceId).executeAsOneOrNull()
        }
        return if (entity != null) {
            InsertResult.Exists
        } else {
            withContext(dispatcherProvider.data) {
                deviceEntityQueries.insert(device.deviceId, device.deviceName, device.platform)
            }
            InsertResult.New
        }
    }

    override suspend fun insertDeviceApp(
        deviceId: DeviceId,
        app: DeviceAppDomainModel
    ): InsertResult {
        val entity = deviceAppEntityQueries.fromDeviceAndPackage(deviceId, app.packageName).executeAsOneOrNull()
        return if (entity != null) {
            InsertResult.Exists
        } else {
            withContext(dispatcherProvider.data) {
                deviceAppEntityQueries.insert(
                    deviceId = deviceId,
                    name = app.name,
                    packageName = app.packageName,
                    iconEncoded = app.iconEncoded,
                    lastAppInstance = app.lastAppInstance,
                    floconVersionOnDevice = app.floconVersionOnDevice,
                )
            }
            InsertResult.New
        }
    }

    override fun observeDeviceApps(deviceId: DeviceId): Flow<List<DeviceAppDomainModel>> {
        return deviceAppEntityQueries.fromDevice(deviceId)
            .asFlow()
            .mapToList(dispatcherProvider.data)
            .map { items ->
                items.map { it.toModel() }
            }
    }

    override suspend fun getDeviceAppByPackage(
        deviceId: DeviceId,
        packageName: AppPackageName
    ): DeviceAppDomainModel? = withContext(dispatcherProvider.data) {
        deviceAppEntityQueries.fromDeviceAndPackage(deviceId, packageName)
            .executeAsOneOrNull()
            ?.toModel()
    }

    override fun observeDeviceAppByPackage(
        deviceId: DeviceId,
        packageName: AppPackageName
    ): Flow<DeviceAppDomainModel?> {
        return deviceAppEntityQueries.fromDeviceAndPackage(deviceId, packageName)
            .asFlow()
            .mapToOneOrNull(dispatcherProvider.data)
            .map { it?.toModel() }
    }

    override suspend fun delete(deviceId: DeviceId) {
        withContext(dispatcherProvider.data) {
            deviceEntityQueries.delete(deviceId)
        }
    }

    override suspend fun deleteApp(
        deviceId: DeviceId,
        packageName: AppPackageName
    ) {
        withContext(dispatcherProvider.data) {
            deviceAppEntityQueries.delete(deviceId, packageName)
        }
    }

    override suspend fun clear() {
        withContext(dispatcherProvider.data) {
            deviceEntityQueries.clear()
        }
    }

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

    // endregion no-op

    // region utils

    private fun DeviceEntity.toModel(): DeviceDomainModel {
        return DeviceDomainModel(
            deviceId = deviceId,
            deviceName = deviceName,
            platform = platform,
        )
    }

    private fun DeviceAppEntity.toModel(): DeviceAppDomainModel {
        return DeviceAppDomainModel(
            name = name,
            packageName = packageName,
            iconEncoded = iconEncoded,
            lastAppInstance = lastAppInstance,
            floconVersionOnDevice = floconVersionOnDevice,
        )
    }

    // endregion utils
}