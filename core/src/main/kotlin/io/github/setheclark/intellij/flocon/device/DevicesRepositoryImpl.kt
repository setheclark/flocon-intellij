package io.github.setheclark.intellij.flocon.device

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.device.datasource.local.LocalCurrentDeviceDataSource
import io.github.openflocon.data.core.device.datasource.local.LocalDevicesDataSource
import io.github.openflocon.data.core.device.datasource.local.model.InsertResult
import io.github.openflocon.data.core.device.datasource.remote.RemoteDeviceDataSource
import io.github.openflocon.domain.Protocol
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.models.AppPackageName
import io.github.openflocon.domain.device.models.DeviceAppDomainModel
import io.github.openflocon.domain.device.models.DeviceDomainModel
import io.github.openflocon.domain.device.models.DeviceId
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.device.models.HandleDeviceResultDomainModel
import io.github.openflocon.domain.device.models.RegisterDeviceWithAppDomainModel
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
class DevicesRepositoryImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val adbRepository: AdbRepository,
    private val remoteDeviceDataSource: RemoteDeviceDataSource,
    private val localDevicesDataSource: LocalDevicesDataSource,
    private val localCurrentDeviceDataSource: LocalCurrentDeviceDataSource,
) : DevicesRepository, MessagesReceiverRepository {

    private val log = Logger.withPluginTag("DevicesRepository")

    private val devicesMutex = Mutex()

    override val devices: Flow<List<DeviceDomainModel>> = localDevicesDataSource.devices
        .flowOn(dispatcherProvider.data)

    override val currentDeviceId: Flow<DeviceId?> = localCurrentDeviceDataSource.currentDeviceId

    override val activeDevices = remoteDeviceDataSource.activeDevices
        .flowOn(dispatcherProvider.data)

    override suspend fun getCurrentDeviceId(): DeviceId? = localCurrentDeviceDataSource.getCurrentDeviceId()

    override suspend fun getCurrentDevice(): DeviceDomainModel? =
        localCurrentDeviceDataSource.getCurrentDeviceId()?.let {
            localDevicesDataSource.getDeviceById(it)
        }

    override fun observeCurrentDevice(): Flow<DeviceDomainModel?> =
        localCurrentDeviceDataSource.currentDeviceId.flatMapLatest {
            if (it == null) {
                flowOf(null)
            } else {
                localDevicesDataSource.observeDeviceById(it)
            }
        }

    override suspend fun register(registerDeviceWithApp: RegisterDeviceWithAppDomainModel): HandleDeviceResultDomainModel =
        withContext(dispatcherProvider.data) {
            devicesMutex.withLock {
                val isKnownDevice = localCurrentDeviceDataSource.isKnownDeviceForThisSession(
                    registerDeviceWithApp.device.deviceId,
                )
                val isKnownApp = localCurrentDeviceDataSource.isKnownAppForThisSession(
                    deviceIdAndPackageName = registerDeviceWithApp.deviceIdAndPackageName,
                )
                if (isKnownDevice && isKnownApp) {
                    HandleDeviceResultDomainModel(
                        deviceId = registerDeviceWithApp.device.deviceId,
                        justConnectedForThisSession = false,
                        isNewDevice = false,
                        isNewApp = false,
                    )
                } else {
                    val isNewDevice = when (
                        localDevicesDataSource.insertDevice(
                            registerDeviceWithApp.device,
                        )
                    ) {
                        InsertResult.New -> true
                        InsertResult.Exists, InsertResult.Updated -> false
                    }
                    localCurrentDeviceDataSource.addNewDeviceConnectedForThisSession(
                        registerDeviceWithApp.device.deviceId,
                    )

                    val isNewApp = when (
                        localDevicesDataSource.insertDeviceApp(
                            deviceId = registerDeviceWithApp.device.deviceId,
                            app = registerDeviceWithApp.app,
                        )
                    ) {
                        InsertResult.New, InsertResult.Updated -> true
                        InsertResult.Exists -> false
                    }

                    localCurrentDeviceDataSource.addNewDeviceAppConnectedForThisSession(
                        deviceIdAndPackageName = registerDeviceWithApp.deviceIdAndPackageName,
                    )

                    HandleDeviceResultDomainModel(
                        deviceId = registerDeviceWithApp.device.deviceId,
                        justConnectedForThisSession = true,
                        isNewDevice = isNewDevice,
                        isNewApp = isNewApp,
                    )
                }
            }
        }

    override suspend fun selectDevice(deviceId: DeviceId) {
        withContext(dispatcherProvider.data) {
            localCurrentDeviceDataSource.selectDevice(deviceId)
        }
    }

    override fun observeDeviceApps(deviceId: DeviceId): Flow<List<DeviceAppDomainModel>> =
        localDevicesDataSource.observeDeviceApps(deviceId)
            .flowOn(dispatcherProvider.data)

    override fun observeDeviceSelectedApp(deviceId: DeviceId): Flow<DeviceAppDomainModel?> =
        localCurrentDeviceDataSource.observeDeviceSelectedApp(deviceId)
            .flatMapLatest { packageName ->
                packageName?.let {
                    localDevicesDataSource.observeDeviceAppByPackage(
                        deviceId = deviceId,
                        packageName = it,
                    )
                } ?: flowOf(null)
            }
            .flowOn(dispatcherProvider.data)

    override suspend fun getDeviceSelectedApp(deviceId: DeviceId): DeviceAppDomainModel? =
        withContext(dispatcherProvider.data) {
            localCurrentDeviceDataSource.getDeviceSelectedApp(deviceId)?.let { packageName ->
                localDevicesDataSource.getDeviceAppByPackage(deviceId = deviceId, packageName = packageName)
            }
        }

    override suspend fun getDeviceAppByPackage(
        deviceId: DeviceId,
        appPackageName: String,
    ): DeviceAppDomainModel? = withContext(dispatcherProvider.data) {
        localDevicesDataSource.getDeviceAppByPackage(deviceId, appPackageName)
    }

    override suspend fun selectApp(deviceId: DeviceId, app: DeviceAppDomainModel) {
        withContext(dispatcherProvider.data) {
            localCurrentDeviceDataSource.selectApp(deviceId = deviceId, packageName = app.packageName)
        }
    }

    override suspend fun deleteDevice(deviceId: DeviceId) {
        withContext(dispatcherProvider.data) {
            localDevicesDataSource.delete(deviceId)
            localCurrentDeviceDataSource.delete(deviceId)
        }
    }

    override suspend fun deleteApplication(
        deviceId: DeviceId,
        packageName: AppPackageName,
    ) {
        withContext(dispatcherProvider.data) {
            localDevicesDataSource.deleteApp(
                deviceId = deviceId,
                packageName = packageName,
            )
            localCurrentDeviceDataSource.deleteApp(
                deviceId = deviceId,
                packageName = packageName,
            )
        }
    }

    override suspend fun clear() {
        withContext(dispatcherProvider.data) {
            devicesMutex.withLock {
                localDevicesDataSource.clear()
            }
        }
    }

    // region MessagesReceiverRepository

    override val pluginName: List<String> = listOf(Protocol.FromDevice.Device.Plugin)

    override suspend fun onMessageReceived(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        message: FloconIncomingMessageDomainModel,
    ) {
        when (message.method) {
            Protocol.FromDevice.Device.Method.RegisterDevice -> {
                remoteDeviceDataSource.getDeviceSerial(message)?.let { serial ->
                    adbRepository.saveAdbSerial(
                        deviceId = message.deviceId,
                        serial = serial,
                    )
                }
            }
        }
    }

    override suspend fun onDeviceConnected(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        isNewDevice: Boolean,
    ) = Unit

    // endregion MessagesReceiverRepository

    // region no-op

    override fun observeDeviceSdkVersion(
        deviceId: DeviceId,
        appPackageName: String,
    ): Flow<String?> {
        log.w { "no-op:observeDeviceSdkVersion: $deviceId|$appPackageName" }
        return flowOf(null)
    }

    override suspend fun saveAppIcon(
        deviceId: DeviceId,
        appPackageName: String,
        iconEncoded: String,
    ) {
        log.w { "no-op:saveAppIcon: $deviceId|$appPackageName|$iconEncoded" }
    }

    override suspend fun hasAppIcon(
        deviceId: DeviceId,
        appPackageName: String,
    ): Boolean {
        log.w { "no-op:hasAppIcon: $deviceId|$appPackageName" }
        return false
    }

    override suspend fun askForDeviceAppIcon(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        log.w { "no-op:askForDeviceAppIcon: $deviceIdAndPackageName" }
    }

    override suspend fun restartApp(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        log.w { "no-op:restartApp: $deviceIdAndPackageName" }
    }

    // endregion no-op
}
