package io.github.setheclark.intellij.network.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel
import io.github.openflocon.domain.network.models.NetworkFilterDomainModel
import io.github.openflocon.domain.network.models.NetworkSortDomainModel
import io.github.setheclark.intellij.network.PluginNetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Inject
class ObserveNetworkRequestsUseCase(
    private val networkRepository: PluginNetworkRepository,
) {
    operator fun invoke(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel?,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel,
    ): Flow<List<FloconNetworkCallDomainModel>> = if (deviceIdAndPackageName == null) {
        flowOf(emptyList())
    } else {
        networkRepository.requestsFlow(
            deviceIdAndPackageName = deviceIdAndPackageName,
            sortedBy = sortedBy,
            filter = filter,
        )
    }
}