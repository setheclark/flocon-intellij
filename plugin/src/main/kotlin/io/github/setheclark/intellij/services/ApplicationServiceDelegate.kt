package io.github.setheclark.intellij.services

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.settings.usecase.InitAdbPathUseCase
import io.github.openflocon.domain.settings.usecase.StartAdbForwardUseCase
import io.github.setheclark.intellij.adb.AdbStatusDataSource
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.flocon.device.EnsureSelectedDeviceAndPackageUseCase
import io.github.setheclark.intellij.flocon.messages.MessageServerDelegate
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
class ApplicationServiceDelegate(
    @param:AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val messageServerDelegate: MessageServerDelegate,
    private val adbStatusDataSource: AdbStatusDataSource,
    private val initAdbPathUseCase: InitAdbPathUseCase,
    private val startAdbForwardUseCase: StartAdbForwardUseCase,
    private val ensureSelectedDeviceAndPackageUseCase: EnsureSelectedDeviceAndPackageUseCase,
) {
    private val log = Logger.withPluginTag("ApplicationServiceDelegate")

    fun initialize() {
        coroutineScope.launch {
            log.i { "Starting application service" }

            log.i { "Initializing adb path" }
            initAdbPathUseCase().alsoFailure {
                // Should be in a usecase, but I'd rather reuse InitAdbPathUseCase and be lazy.
                adbStatusDataSource.adbNotFound()
            }

            messageServerDelegate.initialize()

            launch {
                while (isActive) {
                    startAdbForwardUseCase()
                    delay(1_500)
                }
            }

            ensureSelectedDeviceAndPackageUseCase()
        }
    }
}