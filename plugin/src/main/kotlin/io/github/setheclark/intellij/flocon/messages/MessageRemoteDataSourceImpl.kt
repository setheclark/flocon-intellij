package io.github.setheclark.intellij.flocon.messages

import com.flocon.data.remote.server.Server
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.messages.datasource.MessageRemoteDataSource
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.openflocon.domain.messages.models.FloconReceivedFileDomainModel
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.server.MessageServerStatusDataSource
import io.github.setheclark.intellij.server.PluginMessageRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
class MessageRemoteDataSourceImpl(
    private val server: Server,
    private val messageServerStatusDataSource: MessageServerStatusDataSource,
) : MessageRemoteDataSource, PluginMessageRemoteDataSource {

    override fun startServer() {
        synchronized(messageServerStatusDataSource) {
            val current = messageServerStatusDataSource.status.value
            if (current is MessageServerState.Running || current is MessageServerState.Starting) return
            messageServerStatusDataSource.updateStatus(MessageServerState.Starting)
        }
        try {
            server.startWebsocket()
            server.starHttp()
            messageServerStatusDataSource.updateStatus(MessageServerState.Running)
        } catch (t: Throwable) {
            messageServerStatusDataSource.updateStatus(MessageServerState.Error(t.message ?: "Unknown error"))
        }
    }

    override fun stopServer() {
        messageServerStatusDataSource.updateStatus(MessageServerState.Stopping)
        server.stop()
        messageServerStatusDataSource.updateStatus(MessageServerState.Stopped)
    }

    override fun listenMessages(): Flow<FloconIncomingMessageDomainModel> = server.receivedMessages
        .map {
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
