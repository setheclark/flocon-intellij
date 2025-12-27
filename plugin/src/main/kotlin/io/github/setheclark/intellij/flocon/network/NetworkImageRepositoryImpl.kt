package io.github.setheclark.intellij.flocon.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel
import io.github.openflocon.domain.network.repository.NetworkImageRepository

@Inject
@SingleIn(AppScope::class)
class NetworkImageRepositoryImpl : NetworkImageRepository {
    override suspend fun onImageReceived(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        call: FloconNetworkCallDomainModel
    ) {
        Logger.withTag("NetworkImageRepository").w { "onImageReceived: ${deviceIdAndPackageName.deviceId}" }
    }
}