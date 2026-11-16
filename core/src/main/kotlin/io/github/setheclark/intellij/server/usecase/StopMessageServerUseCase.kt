package io.github.setheclark.intellij.server.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.messages.repository.MessagesRepository

@Inject
class StopMessageServerUseCase(
    private val messagesRepository: MessagesRepository,
) {
    operator fun invoke() {
        messagesRepository.stopServer()
    }
}
