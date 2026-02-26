package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.usecase.SelectDeviceAppUseCase
import io.github.openflocon.domain.device.usecase.SelectDeviceUseCase

@Inject
class SelectDeviceAndAppUseCase(
    private val selectDeviceUseCase: SelectDeviceUseCase,
    private val selectDeviceAppUseCase: SelectDeviceAppUseCase,
) {
    suspend operator fun invoke(deviceId: String, packageName: String) {
        selectDeviceUseCase(deviceId)
        selectDeviceAppUseCase(packageName)
    }
}
