package io.github.setheclark.intellij.server

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.messages.usecase.HandleIncomingMessagesUseCase
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.server.usecase.StopMessageServerUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Inject
class MessageServerDelegate(
    @param:AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val startServerUseCase: StartServerUseCase,
    private val stopServerUseCase: StopMessageServerUseCase,
    private val handleIncomingMessagesUseCase: HandleIncomingMessagesUseCase,
) {
    fun initialize() {
        coroutineScope.launch {
            handleIncomingMessagesUseCase()
                .collect()
        }

        coroutineScope.launch { startServerUseCase() }
    }

    fun shutdown() {
        stopServerUseCase()
    }
}