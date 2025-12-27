package io.github.setheclark.intellij.flocon.adb

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.Either
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.common.Success
import io.github.setheclark.intellij.adb.AdbStatusManager

@Inject
class InitAdbPathUseCase(
    private val adbStatusManager: AdbStatusManager,
    private val adbRepository: AdbRepository,
) {
    operator fun invoke(): Either<Throwable, Unit> {
        val path = adbRepository.findAdbPath()
        return if (path != null) {
            adbStatusManager.setAdbPath(path)
            Success(Unit)
        } else {
            adbStatusManager.adbNotFound()
            Failure(Throwable("adb not found"))
        }
    }
}