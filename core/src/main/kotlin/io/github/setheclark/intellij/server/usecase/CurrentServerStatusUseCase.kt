package io.github.setheclark.intellij.server.usecase

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.server.MessageServerStatusDataSource
import kotlinx.coroutines.flow.Flow

@Inject
class CurrentServerStatusUseCase(
    private val messageServerStatusDataSource: MessageServerStatusDataSource,
) {
    operator fun invoke(): Flow<MessageServerState> = messageServerStatusDataSource.status
}
