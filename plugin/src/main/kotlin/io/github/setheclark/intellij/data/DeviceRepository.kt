package io.github.setheclark.intellij.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.domain.models.ConnectedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing connected device state.
 */
interface DeviceRepository {
    val connectedDevices: StateFlow<Set<ConnectedDevice>>

    fun addDevice(device: ConnectedDevice)
    fun removeDevice(deviceId: String, packageName: String)
    fun updateDevice(deviceId: String, packageName: String, update: (ConnectedDevice) -> ConnectedDevice)
    fun setDevices(devices: Set<ConnectedDevice>)
    fun clear()
}

@Inject
@SingleIn(AppScope::class)
class DeviceRepositoryImpl : DeviceRepository {

    init {
        println("DeviceRepositoryImpl.init ${System.identityHashCode(this)}")
    }

    private val _connectedDevices = MutableStateFlow<Set<ConnectedDevice>>(emptySet())
    override val connectedDevices: StateFlow<Set<ConnectedDevice>> = _connectedDevices.asStateFlow()

    override fun addDevice(device: ConnectedDevice) {
        _connectedDevices.value = _connectedDevices.value + device
    }

    override fun removeDevice(deviceId: String, packageName: String) {
        _connectedDevices.value = _connectedDevices.value.filter {
            it.deviceId != deviceId || it.packageName != packageName
        }.toSet()
    }

    override fun updateDevice(
        deviceId: String,
        packageName: String,
        update: (ConnectedDevice) -> ConnectedDevice
    ) {
        _connectedDevices.value = _connectedDevices.value.map { device ->
            if (device.deviceId == deviceId && device.packageName == packageName) {
                update(device)
            } else {
                device
            }
        }.toSet()
    }

    override fun setDevices(devices: Set<ConnectedDevice>) {
        _connectedDevices.value = devices
    }

    override fun clear() {
        _connectedDevices.value = emptySet()
    }
}
