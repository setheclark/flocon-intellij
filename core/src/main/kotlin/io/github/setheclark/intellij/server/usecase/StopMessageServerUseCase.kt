package io.github.setheclark.intellij.server.usecase

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.server.PluginMessageRemoteDataSource

@Inject
class StopMessageServerUseCase(
    private val messagesRepository: PluginMessageRemoteDataSource,
) {
    operator fun invoke() {
        messagesRepository.stopServer()
    }
}
