package io.github.setheclark.intellij.flocon.messages

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.common.Either
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.common.Success
import io.github.openflocon.domain.messages.usecase.HandleIncomingMessagesUseCase
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Inject
class MessageServerDelegate(
    @param:AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val startServerUseCase: StartServerUseCase,
    private val handleIncomingMessagesUseCase: HandleIncomingMessagesUseCase,
) {
    private val log = Logger.withPluginTag("MessageServerDelegate")

    fun initialize() {
        coroutineScope.launch {
            handleIncomingMessagesUseCase()
                .collect()
        }

        coroutineScope.launch {
            while (isActive) {
                startServer().fold(
                    doOnSuccess = {
                        log.v { "Server started" }
                        delay(20.seconds)
                    },
                    doOnFailure = {
                        log.v { "Server not started" }
                        delay(3.seconds)
                    },
                )
            }
        }
    }

    private fun startServer(): Either<Throwable, Unit> = try {
        startServerUseCase()
        Success(Unit)
    } catch (t: Throwable) {
        Failure(t)
    }
}