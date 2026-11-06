package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.setheclark.intellij.flocon.network.NetworkRepository

/**
 * Clears the network list of the current app session of the given device and app.
 */
@Inject
class ClearNetworkHistoryUseCase(
    private val networkRepository: NetworkRepository,
) {
    suspend operator fun invoke(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        networkRepository.clearHistory(deviceIdAndPackageName.deviceId, deviceIdAndPackageName.packageName)
    }
}
