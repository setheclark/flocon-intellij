package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.server.MessageServerState
import io.github.setheclark.intellij.server.MessageServerStatusDataSource
import kotlinx.coroutines.flow.StateFlow

@Inject
class ObserveServerStatusUseCase(
    private val messageServerStatusDataSource: MessageServerStatusDataSource,
) {
    operator fun invoke(): StateFlow<MessageServerState> = messageServerStatusDataSource.status
}
