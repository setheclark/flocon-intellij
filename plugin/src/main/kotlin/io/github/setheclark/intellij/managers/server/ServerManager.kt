package io.github.setheclark.intellij.managers.server

import co.touchlab.kermit.Logger
import com.flocon.data.remote.server.Server
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.domain.models.ConnectedDevice
import io.github.setheclark.intellij.domain.models.ServerState
import io.github.setheclark.intellij.managers.adb.AdbManager
import io.github.setheclark.intellij.services.ServerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Manages the Flocon server lifecycle.
 * Coordinates starting/stopping the server, observing devices, and routing messages.
 */
@SingleIn(AppScope::class)
@Inject
class ServerManager(
    @param:AppCoroutineScope private val scope: CoroutineScope,
    private val serverFactory: ServerFactory,
    private val messageRouter: MessageRouter,
    private val deviceRepository: DeviceRepository,
    private val adbManager: AdbManager,
    private val json: Json,
) {
    private val log = Logger.withTag("ServerManager")

    private var server: Server? = null

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    /**
     * Start the Flocon WebSocket server on the specified ports.
     */
    fun startServer(websocketPort: Int) {
        if (_serverState.value is ServerState.Running) {
            log.w { "Server already running" }
            return
        }

        log.i { "Starting Flocon server on port $websocketPort" }
        _serverState.value = ServerState.Starting

        try {
            log.i { "Creating Server instance" }
            val newServer = serverFactory.createServer(json)
            server = newServer

            // Start WebSocket server
            log.i { "Calling server.startWebsocket($websocketPort)" }
            newServer.startWebsocket(websocketPort)
            log.i { "WebSocket server started" }

            // Observe connected devices
            scope.launch {
                log.i { "Starting to observe activeDevices" }
                newServer.activeDevices.collect { devices ->
                    log.i { "Active devices changed: ${devices.size} device(s)" }
                    devices.forEach { device ->
                        log.i { "  - deviceId=${device.deviceId}, package=${device.packageName}" }
                    }
                    deviceRepository.setDevices(
                        devices.map { device ->
                            ConnectedDevice(
                                deviceId = device.deviceId,
                                deviceName = "", // Will be populated from messages
                                packageName = device.packageName,
                                appName = "", // Will be populated from messages
                                appInstance = device.appInstance,
                            )
                        }.toSet()
                    )
                }
            }

            // Process incoming messages
            scope.launch {
                log.i { "Starting to collect from server.receivedMessages" }
                try {
                    newServer.receivedMessages.collect { message ->
                        try {
                            log.i { "Received message: plugin=${message.plugin}, method=${message.method}" }
                            messageRouter.routeMessage(message)
                        } catch (e: Exception) {
                            log.e(e) { "Error processing message: ${e.message}" }
                        }
                    }
                } catch (e: Exception) {
                    log.e(e) { "Error collecting from server" }
                }
                log.i { "Stopped collecting from server.receivedMessages" }
            }

            _serverState.value = ServerState.Running(websocketPort)
            log.i { "Flocon server started successfully on port $websocketPort" }

            // Start ADB reverse forwarding
            adbManager.startAdbForwarding(websocketPort)

        } catch (e: Exception) {
            log.e(e) { "Failed to start Flocon server" }
            _serverState.value = ServerState.Error(e.message ?: "Unknown error")
            server?.stop()
            server = null
        }
    }

    /**
     * Stop the Flocon server.
     */
    fun stopServer() {
        log.i { "Stopping Flocon server" }
        _serverState.value = ServerState.Stopping

        // Stop ADB reverse forwarding
        adbManager.stopAdbForwarding()

        server?.stop()
        server = null

        _serverState.value = ServerState.Stopped
        deviceRepository.clear()
    }

    /**
     * Cleanup resources.
     */
    fun dispose() {
        log.i { "ServerManager disposing" }
        stopServer()
    }
}
