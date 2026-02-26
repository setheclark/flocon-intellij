package io.github.setheclark.intellij.flocon.adb

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.adb.datasource.local.AdbLocalDataSource
import io.github.openflocon.domain.adb.model.DeviceWithSerialDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Inject
@SingleIn(AppScope::class)
class InMemoryAdbLocalDataSource : AdbLocalDataSource {

    private val _devicesWithSerial = MutableStateFlow<Set<DeviceWithSerialDomainModel>>(emptySet())

    override val devicesWithSerial: Flow<Set<DeviceWithSerialDomainModel>>
        get() = _devicesWithSerial

    override suspend fun add(item: DeviceWithSerialDomainModel) {
        _devicesWithSerial.value += item
    }

    override suspend fun getFromDeviceId(deviceId: String): DeviceWithSerialDomainModel? {
        return _devicesWithSerial.value.firstOrNull { it.deviceId == deviceId }
    }
}
