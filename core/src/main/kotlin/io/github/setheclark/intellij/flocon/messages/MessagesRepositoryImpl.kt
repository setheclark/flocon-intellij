package io.github.setheclark.intellij.flocon.messages

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.messages.datasource.MessageRemoteDataSource
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.openflocon.domain.messages.models.FloconReceivedFileDomainModel
import io.github.openflocon.domain.messages.repository.MessagesRepository
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
class MessagesRepositoryImpl(
    private val remote: MessageRemoteDataSource,
) : MessagesRepository {

    private val log = Logger.withPluginTag("MessagesRepository")

    override fun startServer() {
        remote.startServer()
    }

    override fun listenMessages(): Flow<FloconIncomingMessageDomainModel> = remote.listenMessages()

    override fun listenReceivedFiles(): Flow<FloconReceivedFileDomainModel> = remote.listenReceivedFiles()
}
