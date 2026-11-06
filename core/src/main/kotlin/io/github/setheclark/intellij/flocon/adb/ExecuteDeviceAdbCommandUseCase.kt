package io.github.setheclark.intellij.flocon.adb

import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.Either
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.settings.repository.SettingsRepository

/**
 * Runs an ADB command on exactly one device.
 *
 * Unlike upstream's ExecuteAdbCommandUseCase, an unknown serial is an error rather than
 * "run on all devices".
 */
class ExecuteDeviceAdbCommandUseCase(
    private val adbRepository: AdbRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        deviceId: String,
        command: String,
    ): Either<Throwable, String> {
        val adbPath = settingsRepository.getAdbPath() ?: return Failure(Throwable("No adb path"))
        val serial = adbRepository.getAdbSerial(deviceId)
            ?: return Failure(Throwable("Unknown ADB serial for device '$deviceId'; refusing to run command"))
        return adbRepository.executeAdbCommand(
            adbPath = adbPath,
            deviceSerial = serial,
            command = command,
        )
    }
}
