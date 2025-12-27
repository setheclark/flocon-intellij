package io.github.setheclark.intellij.flocon.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.network.datasource.NetworkQualityLocalDataSource
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.network.models.BadQualityConfigDomainModel
import io.github.openflocon.domain.network.models.BadQualityConfigId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Inject
class NetworkQualityLocalDataSourceImpl : NetworkQualityLocalDataSource {

    private val log = Logger.withTag("NetworkQualityLocalDataSource")

    override suspend fun save(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        config: BadQualityConfigDomainModel
    ) {
        log.w { "no-op:save: $deviceIdAndPackageName|$config" }
    }

    override suspend fun getNetworkQuality(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        configId: BadQualityConfigId
    ): BadQualityConfigDomainModel? {
        log.w { "no-op:getNetworkQuality: $deviceIdAndPackageName|$configId" }
        return null
    }

    override fun observe(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        configId: BadQualityConfigId
    ): Flow<BadQualityConfigDomainModel?> {
        log.w { "no-op:observe: $deviceIdAndPackageName|$configId" }
        return flowOf(null)
    }

    override suspend fun delete(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        configId: BadQualityConfigId
    ) {
        log.w { "no-op:delete: $deviceIdAndPackageName|$configId" }
    }

    override fun observeAll(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel): Flow<List<BadQualityConfigDomainModel>> {
        log.w { "no-op:observeAll: $deviceIdAndPackageName" }
        return flowOf(emptyList())
    }

    override suspend fun setEnabledConfig(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        configId: BadQualityConfigId?
    ) {
        log.w { "no-op:setEnabledConfig: $deviceIdAndPackageName|$configId" }
    }

    override suspend fun getTheOnlyEnabledNetworkQuality(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel): BadQualityConfigDomainModel? {
        log.w { "no-op:getTheOnlyEnabledNetworkQuality: $deviceIdAndPackageName" }
        return null
    }

    override suspend fun prepopulate(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        log.w { "no-op:prepopulate: $deviceIdAndPackageName" }
    }
}