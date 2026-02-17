package io.github.setheclark.intellij.flocon.device

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.repository.DevicesRepository
import kotlinx.coroutines.flow.first

@Inject
class EnsureSelectedDeviceAndPackageUseCase(
    private val devicesRepository: DevicesRepository,
) {

    suspend operator fun invoke() {
        val deviceId = ensureDeviceSelected()
        ensureAppSelected(deviceId)
    }

    private suspend fun ensureDeviceSelected(): String {
        // Use existing selection if available
        devicesRepository.getCurrentDeviceId()?.let { return it }

        // Wait for at least one device to be available and select it
        val firstDevice = devicesRepository.devices.first { it.isNotEmpty() }.first()

        // Check again before selecting in case another device was selected while waiting
        devicesRepository.getCurrentDeviceId()?.let { return it }

        devicesRepository.selectDevice(firstDevice.deviceId)
        return firstDevice.deviceId
    }

    private suspend fun ensureAppSelected(deviceId: String) {
        // Use existing selection if available
        if (devicesRepository.getDeviceSelectedApp(deviceId) != null) {
            return
        }

        // Wait for at least one app to be available and select it
        val firstApp = devicesRepository.observeDeviceApps(deviceId).first { it.isNotEmpty() }.first()

        // Check again before selecting in case another app was selected while waiting
        if (devicesRepository.getDeviceSelectedApp(deviceId) == null) {
            devicesRepository.selectApp(deviceId, firstApp)
        }
    }
}