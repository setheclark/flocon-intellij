package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.setheclark.intellij.flocon.network.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Observes the current app instance for the given device/package.
 * Used to detect session changes and reset UI state accordingly.
 */
@Inject
class ObserveCurrentAppInstanceUseCase(
    private val networkRepository: NetworkRepository,
) {

    operator fun invoke(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel?,
    ): Flow<String?> = if (deviceIdAndPackageName == null) {
        flowOf(null)
    } else {
        networkRepository.observeCurrentAppInstance(
            deviceIdAndPackageName.deviceId,
            deviceIdAndPackageName.packageName,
        )
    }
}
