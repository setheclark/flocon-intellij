package io.github.setheclark.intellij.server.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.openflocon.domain.models.settings.ServerPortsDomainModel

/**
 * Starts the message server on Flocon's default ports. The IDE plugin has no setting for custom ports.
 */
@Inject
class StartMessageServerUseCase(
    private val startServerUseCase: StartServerUseCase,
) {
    operator fun invoke() {
        val ports = ServerPortsDomainModel()
        startServerUseCase(websocketPort = ports.websocketPort, httpPort = ports.httpPort)
    }
}
