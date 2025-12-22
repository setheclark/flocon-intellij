package io.github.openflocon.intellij.services

import com.flocon.data.remote.models.FloconDeviceIdAndPackageNameDataModel
import com.flocon.data.remote.models.FloconIncomingMessageDataModel
import com.flocon.data.remote.server.Server
import com.flocon.data.remote.server.ServerJvm
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.flocon.data.remote.network.models.FloconNetworkRequestDataModel
import com.flocon.data.remote.network.models.FloconNetworkResponseDataModel
import io.github.openflocon.domain.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Application-level service for Flocon plugin.
 * Manages the WebSocket server that receives traffic from connected devices.
 * The server is shared across all projects in the IDE.
 */
@Service(Service.Level.APP)
class FloconApplicationService : Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var server: Server? = null

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _connectedDevices = MutableStateFlow<Set<ConnectedDevice>>(emptySet())
    val connectedDevices: StateFlow<Set<ConnectedDevice>> = _connectedDevices.asStateFlow()

    // Network events emitted to all project services
    private val _networkRequests = MutableSharedFlow<NetworkRequestEvent>()
    val networkRequests: SharedFlow<NetworkRequestEvent> = _networkRequests.asSharedFlow()

    private val _networkResponses = MutableSharedFlow<NetworkResponseEvent>()
    val networkResponses: SharedFlow<NetworkResponseEvent> = _networkResponses.asSharedFlow()

    init {
        thisLogger().info("FloconApplicationService initialized")
    }

    /**
     * Start the Flocon WebSocket server on the specified port.
     */
    fun startServer(port: Int = DEFAULT_WEBSOCKET_PORT) {
        if (_serverState.value is ServerState.Running) {
            thisLogger().warn("Server already running")
            return
        }

        thisLogger().info("Starting Flocon server on port $port")
        _serverState.value = ServerState.Starting

        try {
            val serverJvm = ServerJvm(json)
            server = serverJvm

            // Start WebSocket server
            serverJvm.startWebsocket(port)

            // Start HTTP file server
            serverJvm.starHttp(DEFAULT_HTTP_PORT)

            // Observe connected devices
            scope.launch {
                serverJvm.activeDevices.collect { devices ->
                    _connectedDevices.value = devices.map { device ->
                        ConnectedDevice(
                            deviceId = device.deviceId,
                            deviceName = "", // Will be populated from messages
                            packageName = device.packageName,
                            appName = "", // Will be populated from messages
                            appInstance = device.appInstance,
                        )
                    }.toSet()
                }
            }

            // Process incoming messages
            scope.launch {
                serverJvm.receivedMessages.collect { message ->
                    handleIncomingMessage(message)
                }
            }

            _serverState.value = ServerState.Running(port)
            thisLogger().info("Flocon server started successfully on port $port")

            // Start ADB reverse forwarding
            service<AdbService>().startAdbForwarding()

        } catch (e: Exception) {
            thisLogger().error("Failed to start Flocon server", e)
            _serverState.value = ServerState.Error(e.message ?: "Unknown error")
            server?.stop()
            server = null
        }
    }

    /**
     * Handle incoming messages from connected devices.
     */
    private suspend fun handleIncomingMessage(message: FloconIncomingMessageDataModel) {
        thisLogger().debug("Received message: plugin=${message.plugin}, method=${message.method}")

        // Update device info with name from message
        updateDeviceInfo(message)

        when (message.plugin) {
            Protocol.FromDevice.Network.Plugin -> handleNetworkMessage(message)
            Protocol.FromDevice.Device.Plugin -> handleDeviceMessage(message)
            else -> {
                thisLogger().debug("Ignoring message from plugin: ${message.plugin}")
            }
        }
    }

    private fun updateDeviceInfo(message: FloconIncomingMessageDataModel) {
        _connectedDevices.value = _connectedDevices.value.map { device ->
            if (device.deviceId == message.deviceId && device.packageName == message.appPackageName) {
                device.copy(
                    deviceName = message.deviceName,
                    appName = message.appName,
                )
            } else {
                device
            }
        }.toSet()
    }

    private suspend fun handleNetworkMessage(message: FloconIncomingMessageDataModel) {
        when (message.method) {
            Protocol.FromDevice.Network.Method.LogNetworkCallRequest -> {
                try {
                    val request = json.decodeFromString<FloconNetworkRequestDataModel>(message.body)
                    val callId = request.floconCallId ?: return
                    _networkRequests.emit(
                        NetworkRequestEvent(
                            deviceId = message.deviceId,
                            packageName = message.appPackageName,
                            appInstance = message.appInstance,
                            callId = callId,
                            request = request,
                        )
                    )
                    thisLogger().debug("Network request: ${request.method} ${request.url}")
                } catch (e: Exception) {
                    thisLogger().error("Failed to parse network request", e)
                }
            }

            Protocol.FromDevice.Network.Method.LogNetworkCallResponse -> {
                try {
                    val response = json.decodeFromString<FloconNetworkResponseDataModel>(message.body)
                    val callId = response.floconCallId ?: return
                    _networkResponses.emit(
                        NetworkResponseEvent(
                            deviceId = message.deviceId,
                            packageName = message.appPackageName,
                            appInstance = message.appInstance,
                            callId = callId,
                            response = response,
                        )
                    )
                    thisLogger().debug("Network response: ${response.responseHttpCode} (${response.durationMs}ms)")
                } catch (e: Exception) {
                    thisLogger().error("Failed to parse network response", e)
                }
            }

            Protocol.FromDevice.Network.Method.LogWebSocketEvent -> {
                // TODO: Handle WebSocket events
                thisLogger().debug("WebSocket event received")
            }

            else -> {
                thisLogger().debug("Unknown network method: ${message.method}")
            }
        }
    }

    private fun handleDeviceMessage(message: FloconIncomingMessageDataModel) {
        when (message.method) {
            Protocol.FromDevice.Device.Method.RegisterDevice -> {
                thisLogger().info("Device registered: ${message.deviceName} (${message.appPackageName})")
            }
            else -> {
                thisLogger().debug("Unknown device method: ${message.method}")
            }
        }
    }

    /**
     * Stop the Flocon WebSocket server.
     */
    fun stopServer() {
        thisLogger().info("Stopping Flocon server")
        _serverState.value = ServerState.Stopping

        // Stop ADB reverse forwarding
        service<AdbService>().stopAdbForwarding()

        server?.stop()
        server = null

        _serverState.value = ServerState.Stopped
        _connectedDevices.value = emptySet()
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

/**
 * Event emitted when a network request is received.
 */
data class NetworkRequestEvent(
    val deviceId: String,
    val packageName: String,
    val appInstance: Long,
    val callId: String,
    val request: FloconNetworkRequestDataModel,
)

/**
 * Event emitted when a network response is received.
 */
data class NetworkResponseEvent(
    val deviceId: String,
    val packageName: String,
    val appInstance: Long,
    val callId: String,
    val response: FloconNetworkResponseDataModel,
)
