package io.github.setheclark.intellij.flocon.adb

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.data.core.adb.datasource.local.AdbLocalDataSource
import io.github.openflocon.domain.adb.model.DeviceWithSerialDomainModel
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.common.Either
import io.github.openflocon.domain.common.Failure
import io.github.openflocon.domain.common.Success
import io.github.setheclark.intellij.process.ProcessExecutor
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@Inject
@SingleIn(AppScope::class)
class AdbRepositoryImpl(
    private val adbLocalDataSource: AdbLocalDataSource,
    private val dispatcherProvider: DispatcherProvider,
    private val processExecutor: ProcessExecutor,
) : AdbRepository {

    private val log = Logger.withPluginTag("AdbRepository")

    override suspend fun getAdbSerial(deviceId: String) = withContext(dispatcherProvider.data) {
        adbLocalDataSource.getFromDeviceId(deviceId)?.serial
    }

    override suspend fun saveAdbSerial(deviceId: String, serial: String) {
        withContext(dispatcherProvider.data) {
            adbLocalDataSource.add(
                DeviceWithSerialDomainModel(
                    deviceId = deviceId,
                    serial = serial,
                ),
            )
        }
    }

    override val devicesWithSerial = adbLocalDataSource.devicesWithSerial.flowOn(dispatcherProvider.data)

    override fun executeAdbCommand(
        adbPath: String,
        deviceSerial: String?,
        command: String
    ): Either<Throwable, String> {
        return if (deviceSerial == null) {
            // When no specific device is targeted, send to all connected devices
            executeAdbAskSerialToAllDevices(adbPath, command, "")
        } else {
            executeAdbCommand("$adbPath -s $deviceSerial", command)
        }
    }

    override fun executeAdbAskSerialToAllDevices(
        adbPath: String,
        command: String,
        serialVariableName: String
    ): Either<Throwable, String> {
        val devices = listConnectedDevices(adbPath)

        if (devices.isEmpty()) {
            return executeAdbCommand(adbPath, command)
        }

        devices.map { serial ->
            executeAdbCommand(
                adbPath = "$adbPath -s $serial",
                command = if (serialVariableName.isNotEmpty()) {
                    command.replace(serialVariableName, serial)
                } else {
                    command
                }
            )
        }.let { results ->
            results.forEach {
                if (it is Failure) {
                    log.e { "ADB command failed: ${it.value}" }
                    return it
                }
            }

            return results.firstOrNull() ?: Success("")
        }
    }

    override fun findAdbPath(): String? {
        // 1. Check if 'adb' is in system PATH
        try {
            val result = processExecutor.execute("adb", "version")

            result.mapSuccess {
                log.d("'adb' found in system PATH.")
                return "adb" // It's in the PATH, so we can just use "adb"
            }
        } catch (e: IOException) {
            log.e(e) { " 'adb' not found in system PATH directly: ${e.message}" }
            // Fall through to search in SDK
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.e(e) { "Process interrupted while checking 'adb' in system PATH." }
        }

        // 2. Check for common environment variable
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adbFromEnv = File(androidHome, "platform-tools/adb")
            if (adbFromEnv.exists() && adbFromEnv.canExecute()) {
                log.d { "Found ADB via ANDROID_HOME: ${adbFromEnv.absolutePath}" }
                return adbFromEnv.absolutePath
            }
        }

        // 3. Search common Android SDK locations
        val userHome = System.getProperty("user.home")
        val possibleSdkPaths = listOf(
            File(userHome, "Library/Android/sdk"),           // macOS default
            File(userHome, "AppData/Local/Android/sdk"),     // Windows default
            File(userHome, "Android/sdk"),                   // Linux common
            File("/usr/local/android-sdk"),                  // Custom install
        )

        for (sdkPath in possibleSdkPaths) {
            val platformToolsPath = File(sdkPath, "platform-tools")
            val adbExecutable = File(platformToolsPath, "adb")
            if (adbExecutable.exists() && adbExecutable.canExecute()) {
                log.d { "Found ADB at: ${adbExecutable.absolutePath}" }
                return adbExecutable.absolutePath
            }
        }

        return null
    }

    private fun executeAdbCommand(
        adbPath: String,
        command: String,
    ): Either<Throwable, String> {
        log.v { "execute command: $adbPath $command" }
        return processExecutor.execute("$adbPath $command")
    }

    private fun listConnectedDevices(adbPath: String): List<String> {
        val result = processExecutor.execute("$adbPath devices")

        return result.map(
            mapError = { emptyList() },
            mapSuccess = { output ->
                output.lines()
                    .filter { line ->
                        line.endsWith("device") && !line.startsWith("List of devices attached")
                    }
                    .mapNotNull { line ->
                        line.split("\t").firstOrNull()
                    }
            }
        )
    }
}