package io.github.setheclark.intellij.managers.adb

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.domain.models.AdbStatus
import io.github.setheclark.intellij.process.ProcessExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manager for ADB operations, particularly reverse TCP forwarding.
 * This enables Android devices to connect to our WebSocket server.
 */
@SingleIn(AppScope::class)
@Inject
class AdbManager(
    @AppCoroutineScope private val scope: CoroutineScope,
    private val processExecutor: ProcessExecutor,
) {
    private val log = Logger.withTag("AdbManager")

    private var adbForwardJob: Job? = null
    private var adbPath: String? = null

    private val _adbStatus = MutableStateFlow<AdbStatus>(AdbStatus.Initializing)
    val adbStatus: StateFlow<AdbStatus> = _adbStatus.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        log.i { "AdbManager initialized" }
        adbPath = findAdbPath()
        if (adbPath != null) {
            log.i { "Found ADB at: $adbPath" }
            _adbStatus.value = AdbStatus.Available(adbPath!!)
        } else {
            log.w { "ADB not found. Reverse TCP forwarding will not be available." }
            _adbStatus.value = AdbStatus.NotFound
        }
    }

    /**
     * Start periodic ADB reverse forwarding.
     * This runs every 1.5 seconds to maintain the reverse connection.
     */
    fun startAdbForwarding(websocketPort: Int) {
        val path = adbPath
        if (path == null) {
            log.w { "Cannot start ADB forwarding - ADB path not found" }
            return
        }

        if (adbForwardJob?.isActive == true) {
            log.d { "ADB forwarding already running" }
            return
        }

        log.i { "Starting ADB reverse forwarding" }
        adbForwardJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    executeAdbReverse(path, websocketPort)
                } catch (e: Exception) {
                    log.d { "ADB reverse failed: ${e.message}" }
                }
                delay(1500) // Run every 1.5 seconds like Flocon
            }
        }
    }

    /**
     * Stop periodic ADB reverse forwarding.
     */
    fun stopAdbForwarding() {
        log.i { "Stopping ADB reverse forwarding" }
        adbForwardJob?.cancel()
        adbForwardJob = null
        if (adbPath != null) {
            _adbStatus.value = AdbStatus.Available(adbPath!!)
        }
    }

    /**
     * Execute ADB reverse for a specific port on all connected devices.
     */
    private fun executeAdbReverse(adbPath: String, port: Int) {
        val devices = listConnectedDevices(adbPath)
        if (devices.isEmpty()) {
            // Run without -s flag if no devices or single device
            executeAdbCommand(adbPath, null, "reverse tcp:$port tcp:$port")
        } else {
            devices.forEach { serial ->
                executeAdbCommand(adbPath, serial, "reverse tcp:$port tcp:$port")
            }
        }
    }

    /**
     * Execute an ADB command.
     */
    private fun executeAdbCommand(adbPath: String, deviceSerial: String?, command: String): Boolean {
        val commandParts = buildList {
            add(adbPath)
            if (deviceSerial != null) {
                add("-s")
                add(deviceSerial)
            }
            addAll(command.split(" "))
        }

        val result = processExecutor.execute(*commandParts.toTypedArray())
        return result.isSuccess
    }

    private fun listConnectedDevices(adbPath: String): List<String> {
        val result = processExecutor.execute(adbPath, "devices")
        if (!result.isSuccess) {
            log.d { "Error listing devices: ${result.errorOutput}" }
            return emptyList()
        }

        return result.output.lines()
            .filter { line ->
                line.endsWith("device") && !line.startsWith("List of devices attached")
            }
            .mapNotNull { line ->
                line.split("\t").firstOrNull()
            }
    }

    /**
     * Find the ADB executable path.
     * Checks system PATH first, then common Android SDK locations.
     */
    fun findAdbPath(): String? {
        // 1. Check if 'adb' is in system PATH
        if (processExecutor.isCommandAvailable("adb")) {
            log.d { "Found 'adb' in system PATH" }
            return "adb"
        }

        // 2. Search common Android SDK locations
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

        // 3. Try Android Studio's bundled ADB via ANDROID_HOME environment variable
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adbFromEnv = File(androidHome, "platform-tools/adb")
            if (adbFromEnv.exists() && adbFromEnv.canExecute()) {
                log.d { "Found ADB via ANDROID_HOME: ${adbFromEnv.absolutePath}" }
                return adbFromEnv.absolutePath
            }
        }

        return null
    }

    /**
     * Cleanup resources.
     */
    fun dispose() {
        log.i { "AdbManager disposing" }
        stopAdbForwarding()
        // Note: Don't cancel scope - it's the app scope, managed by ApplicationService
    }
}
