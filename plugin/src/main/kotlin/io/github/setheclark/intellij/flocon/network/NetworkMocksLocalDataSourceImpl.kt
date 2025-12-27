package io.github.setheclark.intellij.flocon.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.network.datasource.NetworkMocksLocalDataSource
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.network.models.MockNetworkDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Inject
@SingleIn(AppScope::class)
class NetworkMocksLocalDataSourceImpl : NetworkMocksLocalDataSource {
    override suspend fun addMock(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        mock: MockNetworkDomainModel
    ) {

    }

    override suspend fun getMock(id: String): MockNetworkDomainModel? {
        return null
    }

    override suspend fun getAllEnabledMocks(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel): List<MockNetworkDomainModel> {
        return emptyList()
    }

    override suspend fun observeAll(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel): Flow<List<MockNetworkDomainModel>> {
        return flowOf(emptyList())
    }

    override suspend fun deleteMock(id: String) {

    }

    override suspend fun updateMockIsEnabled(id: String, isEnabled: Boolean) {

    }

    override suspend fun updateMockDevice(
        mockId: String,
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel?
    ) {

    }
}