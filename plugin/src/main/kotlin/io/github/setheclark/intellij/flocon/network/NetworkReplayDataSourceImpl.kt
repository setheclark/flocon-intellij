package io.github.setheclark.intellij.flocon.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.network.datasource.NetworkReplayDataSource
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel

@Inject
@SingleIn(AppScope::class)
class NetworkReplayDataSourceImpl : NetworkReplayDataSource {
    override suspend fun replay(request: FloconNetworkCallDomainModel): FloconNetworkCallDomainModel {
        Logger.withTag("NetworkReplayDataSource").w { "no-op:replay: ${request.request.url}" }
        return request
    }
}