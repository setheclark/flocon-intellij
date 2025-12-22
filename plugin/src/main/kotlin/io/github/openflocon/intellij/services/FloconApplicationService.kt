package io.github.openflocon.intellij.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-level service for Flocon plugin.
 * Manages the WebSocket server that receives traffic from connected devices.
 * The server is shared across all projects in the IDE.
 */
@Service(Service.Level.APP)
class FloconApplicationService : Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    init {
        thisLogger().info("FloconApplicationService initialized")
    }

    /**
     * Start the Flocon WebSocket server on the specified port.
     * This will set up ADB reverse port forwarding for connected devices.
     */
    fun startServer(port: Int = DEFAULT_WEBSOCKET_PORT) {
        if (_serverState.value is ServerState.Running) {
            thisLogger().warn("Server already running")
            return
        }

        thisLogger().info("Starting Flocon server on port $port")
        _serverState.value = ServerState.Starting

        // TODO: Wire up Flocon's ServerJvm from data-remote module
        // For now, just mark as running for UI development
        _serverState.value = ServerState.Running(port)
    }

    /**
     * Stop the Flocon WebSocket server.
     */
    fun stopServer() {
        thisLogger().info("Stopping Flocon server")
        _serverState.value = ServerState.Stopping

        // TODO: Stop the actual server
        _serverState.value = ServerState.Stopped
        _connectedDevices.value = emptyList()
    }

    override fun dispose() {
        thisLogger().info("FloconApplicationService disposing")
        stopServer()
        scope.cancel()
    }

    companion object {
        const val DEFAULT_WEBSOCKET_PORT = 9023
        const val DEFAULT_HTTP_PORT = 9024
    }
}

/**
 * Represents the current state of the WebSocket server.
 */
sealed class ServerState {
    data object Stopped : ServerState()
    data object Starting : ServerState()
    data class Running(val port: Int) : ServerState()
    data object Stopping : ServerState()
    data class Error(val message: String) : ServerState()
}

/**
 * Represents a connected device/app.
 */
data class ConnectedDevice(
    val deviceId: String,
    val deviceName: String,
    val packageName: String,
    val appName: String,
    val appInstance: Long,
)
