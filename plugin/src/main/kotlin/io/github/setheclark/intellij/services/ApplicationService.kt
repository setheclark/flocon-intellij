package io.github.setheclark.intellij.services

import co.touchlab.kermit.Logger
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import dev.zacsweers.metro.createGraphFactory
import io.github.openflocon.domain.Constant
import io.github.setheclark.intellij.di.AppGraph
import io.github.setheclark.intellij.domain.models.ConnectedDevice
import io.github.setheclark.intellij.domain.models.ServerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Application-level service for Flocon plugin.
 * Thin orchestrator that delegates to injected managers.
 * The server is shared across all projects in the IDE.
 */
@Service(Service.Level.APP)
class FloconApplicationService : Disposable {

    private val log = Logger.withTag("FloconApplicationService")

    val appGraph: AppGraph

    init {
        log.i { "FloconApplicationService initialized" }
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
    }

    // Delegate to ServerManager
    val serverState: StateFlow<ServerState>
        get() = appGraph.serverManager.serverState

    // Delegate to DeviceRepository
    val connectedDevices: StateFlow<Set<ConnectedDevice>>
        get() = appGraph.deviceRepository.connectedDevices

    /**
     * Start the Flocon WebSocket server on the specified port.
     */
    fun startServer(port: Int = Constant.SERVER_WEBSOCKET_PORT) {
        appGraph.serverManager.startServer(port)
    }

    /**
     * Stop the Flocon WebSocket server.
     */
    fun stopServer() {
        appGraph.serverManager.stopServer()
    }

    override fun dispose() {
        log.i { "FloconApplicationService disposing" }
        appGraph.serverManager.dispose()
        appGraph.adbManager.dispose()
    }
}
