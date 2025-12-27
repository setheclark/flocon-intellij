package io.github.setheclark.intellij.services

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.settings.usecase.StartAdbForwardUseCase
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.flocon.adb.InitAdbPathUseCase
import io.github.setheclark.intellij.flocon.messages.MessageServerDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
class ApplicationServiceDelegate(
    @param:AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val messageServerDelegate: MessageServerDelegate,
    private val initAdbPathUseCase: InitAdbPathUseCase,
    private val startAdbForwardUseCase: StartAdbForwardUseCase,
) {

    private val log = Logger.withTag("ApplicationServiceDelegate")

    fun initialize() {
        coroutineScope.launch {
            log.i { "Starting application service" }

            log.i { "Initializing adb path" }
            initAdbPathUseCase()

            messageServerDelegate.initialize()

            launch {
                while (isActive) {
                    startAdbForwardUseCase()
                    delay(1_500)
                }
            }
        }
    }
}