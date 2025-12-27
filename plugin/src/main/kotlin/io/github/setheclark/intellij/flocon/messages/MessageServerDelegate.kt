package io.github.setheclark.intellij.flocon.messages

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.messages.usecase.HandleIncomingMessagesUseCase
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.setheclark.intellij.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Inject
class MessageServerDelegate(
    private val startServerUseCase: StartServerUseCase,
    private val handleIncomingMessagesUseCase: HandleIncomingMessagesUseCase,
    @param:AppCoroutineScope private val coroutineScope: CoroutineScope,
) {

    fun initialize() {
        coroutineScope.launch {
            handleIncomingMessagesUseCase().collect()
        }

        coroutineScope.launch {
            while (isActive) {
                try {
                    startServerUseCase()
                    delay(20.seconds)
                } catch (t: Throwable) {
                    delay(3.seconds)
                }
            }
        }
    }
}