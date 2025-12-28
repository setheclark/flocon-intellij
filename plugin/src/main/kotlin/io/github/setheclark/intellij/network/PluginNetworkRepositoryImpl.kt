package io.github.setheclark.intellij.network

import androidx.paging.PagingData
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.flocon.network.NetworkLocalDataSourceImpl
import io.github.openflocon.data.core.network.repository.NetworkRepositoryImpl
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel
import io.github.openflocon.domain.network.models.NetworkFilterDomainModel
import io.github.openflocon.domain.network.models.NetworkSortDomainModel
import io.github.openflocon.domain.network.repository.NetworkBadQualityRepository
import io.github.openflocon.domain.network.repository.NetworkMocksRepository
import io.github.openflocon.domain.network.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PluginNetworkRepositoryImpl(
    private val delegate: NetworkRepositoryImpl,
    private val networkLocalDataSource: NetworkLocalDataSourceImpl,
) : PluginNetworkRepository,
    NetworkRepository by delegate,
    NetworkMocksRepository by delegate,
    MessagesReceiverRepository by delegate,
    NetworkBadQualityRepository by delegate {

    override fun observeRequests(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel
    ): Flow<PagingData<FloconNetworkCallDomainModel>> {
        return flowOf(PagingData.empty())
    }

    override fun requestsFlow(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel
    ): Flow<List<FloconNetworkCallDomainModel>> {
        return networkLocalDataSource.requestsFlow(deviceIdAndPackageName, sortedBy, filter)
    }
}