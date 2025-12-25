package io.github.setheclark.intellij.services

import com.intellij.openapi.diagnostic.thisLogger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Service for managing ADB operations, particularly reverse TCP forwarding.
 * This enables Android devices to connect to our WebSocket server.
 */
@SingleIn(AppScope::class)
@Inject
class AdbService {

    // Dependencies with defaults - can be overridden for testing via companion object
    internal var processExecutor: ProcessExecutor = SystemProcessExecutor()
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO

    private val scope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + dispatcher) }
    private var adbForwardJob: Job? = null
    private var adbPath: String? = null

    private val _adbStatus = MutableStateFlow<AdbStatus>(AdbStatus.Initializing)
    val adbStatus: StateFlow<AdbStatus> = _adbStatus.asStateFlow()

    init {
        initialize()
    }

    /**
     * Initialize the service by finding the ADB path.
     */
    private fun initialize() {
        thisLogger().info("AdbService initialized")
        adbPath = findAdbPath()
        if (adbPath != null) {
            thisLogger().info("Found ADB at: $adbPath")
            _adbStatus.value = AdbStatus.Available(adbPath!!)
        } else {
            thisLogger().warn("ADB not found. Reverse TCP forwarding will not be available.")
            _adbStatus.value = AdbStatus.NotFound
        }
    }

    /**
     * Start periodic ADB reverse forwarding.
     * This runs every 1.5 seconds to maintain the reverse connection.
     */
    fun startAdbForwarding() {
        val path = adbPath
        if (path == null) {
            thisLogger().warn("Cannot start ADB forwarding - ADB path not found")
            return
        }

        if (adbForwardJob?.isActive == true) {
            thisLogger().debug("ADB forwarding already running")
            return
        }

        thisLogger().info("Starting ADB reverse forwarding")
        _adbStatus.value = AdbStatus.Forwarding
        adbForwardJob = scope.launch {
            while (isActive) {
                try {
                    executeAdbReverse(path, FloconApplicationService.DEFAULT_WEBSOCKET_PORT)
                    executeAdbReverse(path, FloconApplicationService.DEFAULT_HTTP_PORT)
                } catch (e: Exception) {
                    thisLogger().debug("ADB reverse failed: ${e.message}")
                }
                delay(1500) // Run every 1.5 seconds like Flocon
            }
        }
    }

    /**
     * Stop periodic ADB reverse forwarding.
     */
    fun stopAdbForwarding() {
        thisLogger().info("Stopping ADB reverse forwarding")
        adbForwardJob?.cancel()
        adbForwardJob = null
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

    /**
     * List connected ADB devices.
     */
    private fun listConnectedDevices(adbPath: String): List<String> {
        val result = processExecutor.execute(adbPath, "devices")
        if (!result.isSuccess) {
            thisLogger().debug("Error listing devices: ${result.errorOutput}")
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
    private fun findAdbPath(): String? {
        // 1. Check if 'adb' is in system PATH
        if (processExecutor.isCommandAvailable("adb")) {
            thisLogger().debug("Found 'adb' in system PATH")
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
                thisLogger().debug("Found ADB at: ${adbExecutable.absolutePath}")
                return adbExecutable.absolutePath
            }
        }

        // 3. Try Android Studio's bundled ADB via ANDROID_HOME environment variable
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adbFromEnv = File(androidHome, "platform-tools/adb")
            if (adbFromEnv.exists() && adbFromEnv.canExecute()) {
                thisLogger().debug("Found ADB via ANDROID_HOME: ${adbFromEnv.absolutePath}")
                return adbFromEnv.absolutePath
            }
        }

        return null
    }

    /**
     * Check if ADB is available.
     */
    fun isAdbAvailable(): Boolean = adbPath != null

//    override fun dispose() {
//        thisLogger().info("AdbService disposing")
//        stopAdbForwarding()
//        scope.cancel()
//    }
}

/**
 * Represents the current status of ADB availability.
 */
sealed class AdbStatus {
    data object Initializing : AdbStatus()
    data class Available(val path: String) : AdbStatus()
    data object NotFound : AdbStatus() {
        val message: String = "ADB not found. USB device connections require ADB.\n\n" +
                "To fix this, either:\n" +
                "• Add 'adb' to your system PATH\n" +
                "• Set ANDROID_HOME or ANDROID_SDK_ROOT environment variable\n" +
                "• Install Android SDK in ~/Library/Android/sdk (macOS)\n\n" +
                "WiFi connections will still work if the device can reach this computer."
    }

    data object Forwarding : AdbStatus()
}
