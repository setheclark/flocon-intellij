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
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Paths

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
        log.d { "Executing ADB command: $command ${deviceSerial?.let { "on device: $it" } ?: "on all devices"}" }
        return if (deviceSerial == null) {
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
            log.d { "No devices connected, executing command without device serial" }
            return executeAdbCommand(adbPath, command)
        }

        log.d { "Executing command on ${devices.size} device(s)" }

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
                    log.e { "ADB command failed: ${it.value.message}" }
                    return it
                }
            }

            return results.firstOrNull() ?: Success("")
        }
    }

    override fun findAdbPath(): String? {
        log.d { "Searching for ADB executable" }

        System.getProperty("android.adb.path")?.let { path ->
            val adb = Paths.get(path)
            if (Files.exists(adb, LinkOption.NOFOLLOW_LINKS)) {
                log.d { "Found ADB from android.adb.path property: $path" }
                return path
            }
        }

        System.getenv("PATH")?.let { pathProperty ->
            pathProperty.split(File.pathSeparatorChar).forEach { dir ->
                val adb = Paths.get(dir, ADB)
                if (Files.exists(adb, LinkOption.NOFOLLOW_LINKS)) {
                    val adbPath = adb.toAbsolutePath().toString()
                    log.d { "Found ADB in PATH: $adbPath" }
                    return adbPath
                }
            }
        }

        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adbFromEnv = File(androidHome, "platform-tools/$ADB")
            if (adbFromEnv.exists() && adbFromEnv.canExecute()) {
                log.d { "Found ADB via ANDROID_HOME/ANDROID_SDK_ROOT: ${adbFromEnv.absolutePath}" }
                return adbFromEnv.absolutePath
            }
        }

        val userHome = System.getProperty("user.home")
        val possibleSdkPaths = listOf(
            File(userHome, "Library/Android/sdk"),           // macOS default
            File(userHome, "AppData/Local/Android/sdk"),     // Windows default
            File(userHome, "Android/sdk"),                   // Linux common
            File("/usr/local/android-sdk"),                  // Custom install
        )

        for (sdkPath in possibleSdkPaths) {
            val adbExecutable = File(sdkPath, "platform-tools/$ADB")
            if (adbExecutable.exists() && adbExecutable.canExecute()) {
                log.d { "Found ADB at common SDK location: ${adbExecutable.absolutePath}" }
                return adbExecutable.absolutePath
            }
        }

        log.w { "ADB executable not found. Checked: android.adb.path property, PATH environment, ANDROID_HOME, ANDROID_SDK_ROOT, and common SDK locations" }
        return null
    }

    private fun executeAdbCommand(
        adbPath: String,
        command: String,
    ): Either<Throwable, String> {
        return processExecutor.execute("$adbPath $command")
    }

    private fun listConnectedDevices(adbPath: String): List<String> {
        val result = processExecutor.execute("$adbPath devices")

        return result.map(
            mapError = { error ->
                log.w { "Failed to list connected devices: ${error.message}" }
                emptyList()
            },
            mapSuccess = { output ->
                val devices = output.lines()
                    .filter { line ->
                        line.endsWith("device") && !line.startsWith("List of devices attached")
                    }
                    .mapNotNull { line ->
                        line.split("\t").firstOrNull()
                    }
                log.d { "Found ${devices.size} connected device(s)" }
                devices
            }
        )
    }

    companion object {
        // TODO Find better place for this
        fun currentPlatform() = when (System.getProperty("os.name").lowercase()) {
            "mac os" -> 3
            "windows" -> 2
            "linux" -> 1
            else -> 0
        }

        private val CurrentPlatform = currentPlatform()

        fun ext(windowsExtension: String = ".exe", nonWindowsExtension: String = ""): String {
            return if (CurrentPlatform == 2) windowsExtension else nonWindowsExtension
        }

        private val ADB = "adb${ext()}"
    }
}