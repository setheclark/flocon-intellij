package io.github.setheclark.intellij.services

import co.touchlab.kermit.Logger
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.setheclark.intellij.di.ProjectGraph
import io.github.setheclark.intellij.domain.models.ConnectedDevice
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.domain.models.ServerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Project-level service for Flocon plugin.
 * Manages UI state for a specific project.
 * Delegates to app-scoped repositories for data.
 */
@Service(Service.Level.PROJECT)
class FloconProjectService(private val project: Project) : Disposable {

    private val log = Logger.withTag("FloconProjectService")

    // Dependencies with defaults - can be overridden for testing
    internal var appServiceProvider: () -> FloconApplicationService = { service<FloconApplicationService>() }

    private val appService: FloconApplicationService = appServiceProvider()

    val projectGraph: ProjectGraph = appService.appGraph.createProjectGraph(
        project = project,
        projectService = this,
    )

    // Delegate network calls to app-scoped repository (via appGraph to ensure singleton)
    val networkCalls: StateFlow<List<NetworkCallEntry>>
        get() = appService.appGraph.networkCallRepository.networkCalls

    // Project-local UI state
    private val _selectedCall = MutableStateFlow<NetworkCallEntry?>(null)
    val selectedCall: StateFlow<NetworkCallEntry?> = _selectedCall.asStateFlow()

    private val _filter = MutableStateFlow(NetworkFilter())
    val filter: StateFlow<NetworkFilter> = _filter.asStateFlow()

    // Delegate server state from app service
    val serverState: StateFlow<ServerState> get() = appService.serverState
    val connectedDevices: StateFlow<Set<ConnectedDevice>> get() = appService.connectedDevices

    init {
        initialize()
    }

    /**
     * Initialize the service by starting the server.
     */
    private fun initialize() {
        log.i { "FloconProjectService initialized for project: ${project.name}" }
        // Auto-start server when first project service is created
        appService.startServer()
    }

    /**
     * Select a network call to show in the detail panel.
     */
    fun selectCall(call: NetworkCallEntry?) {
        _selectedCall.value = call
    }

    /**
     * Update the filter criteria.
     */
    fun updateFilter(filter: NetworkFilter) {
        _filter.value = filter
    }

    /**
     * Clear all captured network calls.
     */
    fun clearAll() {
        appService.appGraph.networkCallRepository.clear()
        _selectedCall.value = null
    }

    override fun dispose() {
        log.i { "FloconProjectService disposing for project: ${project.name}" }
    }
}
