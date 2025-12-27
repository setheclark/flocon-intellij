package io.github.setheclark.intellij.flocon.device

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.models.*
import io.github.openflocon.domain.device.repository.DevicesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Inject
@SingleIn(AppScope::class)
class DevicesRepositoryImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val adbRepository: AdbRepository,
) : DevicesRepository {

    private val log = Logger.withTag("DevicesRepository")

    override val devices: Flow<List<DeviceDomainModel>>
        get() {
            log.w("devices")
            return flowOf(emptyList())
        }

    override val currentDeviceId: Flow<DeviceId?>
        get() {
            log.w { "currentDeviceId" }
            return flowOf(null)
        }

    override val activeDevices: Flow<Set<DeviceIdAndPackageNameDomainModel>>
        get() {
            log.w("activeDevices")
            return flowOf(emptySet())
        }

    override suspend fun getCurrentDeviceId(): DeviceId? {
        log.w { "no-op:getCurrentDeviceId" }
        return null
    }

    override suspend fun register(registerDeviceWithApp: RegisterDeviceWithAppDomainModel): HandleDeviceResultDomainModel {
        log.i { "Register: $registerDeviceWithApp" }
        // TODO Revisit to implement isNewDevice and isNewApp


        return HandleDeviceResultDomainModel(
            deviceId = registerDeviceWithApp.device.deviceId,
            justConnectedForThisSession = true,
            isNewDevice = false,
            isNewApp = false,
        )
    }

    override suspend fun getCurrentDevice(): DeviceDomainModel? {
        log.w { "no-op:getCurrentDevice" }
        return null
    }

    override suspend fun selectDevice(deviceId: DeviceId) {
        log.w { "no-op:selectDevice: $deviceId" }
    }

    override fun observeDeviceApps(deviceId: DeviceId): Flow<List<DeviceAppDomainModel>> {
        log.w { "no-op:observeDeviceApps: $deviceId" }
        return flowOf(emptyList())
    }

    override fun observeDeviceSelectedApp(deviceId: DeviceId): Flow<DeviceAppDomainModel?> {
        log.w { "no-op:observeDeviceSelectedApp: $deviceId" }
        return flowOf(null)
    }

    override suspend fun getDeviceSelectedApp(deviceId: DeviceId): DeviceAppDomainModel? {
        log.w { "no-op:getDeviceSelectedApp: $deviceId" }
        return null
    }

    override suspend fun getDeviceAppByPackage(
        deviceId: DeviceId,
        appPackageName: String
    ): DeviceAppDomainModel? {
        log.w { "no-op:getDeviceAppByPackage: $deviceId|$appPackageName" }
        return null
    }

    override suspend fun selectApp(
        deviceId: DeviceId,
        app: DeviceAppDomainModel
    ) {
        log.w { "no-op:selectApp: $deviceId|$app" }
    }

    override fun observeDeviceSdkVersion(
        deviceId: DeviceId,
        appPackageName: String
    ): Flow<String?> {
        log.w { "no-op:observeDeviceSdkVersion: $deviceId|$appPackageName" }
        return flowOf(null)
    }

    override suspend fun deleteDevice(deviceId: DeviceId) {
        log.w { "no-op:deleteDevice: $deviceId" }
    }

    override suspend fun deleteApplication(
        deviceId: DeviceId,
        packageName: AppPackageName
    ) {
        log.w { "no-op:deleteDevice: $deviceId|$packageName" }
    }

    override suspend fun clear() {
        log.w { "no-op:clear" }
    }

    override fun observeCurrentDevice(): Flow<DeviceDomainModel?> {
        log.w { "no-op:observeCurrentDevice" }
        return flowOf(null)
    }

    override suspend fun saveAppIcon(
        deviceId: DeviceId,
        appPackageName: String,
        iconEncoded: String
    ) {
        log.w { "no-op:saveAppIcon: $deviceId|$appPackageName|$iconEncoded" }
    }

    override suspend fun hasAppIcon(
        deviceId: DeviceId,
        appPackageName: String
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