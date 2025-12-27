package io.github.setheclark.intellij.flocon.messages

import com.flocon.data.remote.models.FloconIncomingMessageDataModel
import com.flocon.data.remote.server.Server
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.messages.datasource.MessageRemoteDataSource
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.openflocon.domain.messages.models.FloconReceivedFileDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class MessageRemoteDataSourceImpl(
    private val server: Server
) : MessageRemoteDataSource {
    override fun startServer() {
        server.startWebsocket()
        server.starHttp()
    }

    override fun listenMessages(): Flow<FloconIncomingMessageDomainModel> = server.receivedMessages
        .map(FloconIncomingMessageDataModel::toDomain)

    override fun listenReceivedFiles(): Flow<FloconReceivedFileDomainModel> = server.receivedFiles

}

// TODO Move?
private fun FloconIncomingMessageDataModel.toDomain() = FloconIncomingMessageDomainModel(
    deviceName = deviceName,
    deviceId = deviceId,
    appName = appName,
    appPackageName = appPackageName,
    method = method,
    body = body,
    plugin = plugin,
    appInstance = appInstance,
    platform = platform,
    floconVersionOnDevice = versionName,
)
