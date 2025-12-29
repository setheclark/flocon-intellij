package io.github.setheclark.intellij.flocon.messages

import co.touchlab.kermit.Logger
import com.flocon.data.remote.server.Server
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.messages.datasource.MessageRemoteDataSource
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.openflocon.domain.messages.models.FloconReceivedFileDomainModel
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
class MessageRemoteDataSourceImpl(
    private val server: Server,
) : MessageRemoteDataSource {

    private val log = Logger.withPluginTag("MessageRemoteDataSource")

    override fun startServer() {
        log.v { "Starting server" }
        server.startWebsocket()
        server.starHttp()
    }

    override fun listenMessages(): Flow<FloconIncomingMessageDomainModel> = server.receivedMessages
        .map {
            log.v { "message: ${it.plugin}" }
            FloconIncomingMessageDomainModel(
                deviceName = it.deviceName,
                deviceId = it.deviceId,
                appName = it.appName,
                appPackageName = it.appPackageName,
                method = it.method,
                body = it.body,
                plugin = it.plugin,
                appInstance = it.appInstance,
                platform = it.platform,
                floconVersionOnDevice = it.versionName,
            )
        }

    override fun listenReceivedFiles(): Flow<FloconReceivedFileDomainModel> = server.receivedFiles
}
