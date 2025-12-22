package io.github.openflocon.intellij.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Project-level service for Flocon plugin.
 * Manages network call data and UI state for a specific project.
 */
@Service(Service.Level.PROJECT)
class FloconProjectService(private val project: Project) : Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val appService = service<FloconApplicationService>()

    private val _networkCalls = MutableStateFlow<List<NetworkCallEntry>>(emptyList())
    val networkCalls: StateFlow<List<NetworkCallEntry>> = _networkCalls.asStateFlow()

    private val _selectedCall = MutableStateFlow<NetworkCallEntry?>(null)
    val selectedCall: StateFlow<NetworkCallEntry?> = _selectedCall.asStateFlow()

    private val _filter = MutableStateFlow(NetworkFilter())
    val filter: StateFlow<NetworkFilter> = _filter.asStateFlow()

    // Delegate server state from app service
    val serverState: StateFlow<ServerState> = appService.serverState
    val connectedDevices: StateFlow<List<ConnectedDevice>> = appService.connectedDevices

    init {
        thisLogger().info("FloconProjectService initialized for project: ${project.name}")
        // Auto-start server when first project service is created
        appService.startServer()
    }

    /**
     * Add a new network call entry.
     * Called when a request is received from a connected device.
     */
    fun addNetworkCall(call: NetworkCallEntry) {
        _networkCalls.value = _networkCalls.value + call
    }

    /**
     * Update an existing network call with response data.
     */
    fun updateNetworkCall(callId: String, update: (NetworkCallEntry) -> NetworkCallEntry) {
        _networkCalls.value = _networkCalls.value.map { call ->
            if (call.id == callId) update(call) else call
        }
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
        _networkCalls.value = emptyList()
        _selectedCall.value = null
    }

    override fun dispose() {
        thisLogger().info("FloconProjectService disposing for project: ${project.name}")
        scope.cancel()
    }
}

/**
 * Represents a captured network call (request + optional response).
 */
data class NetworkCallEntry(
    val id: String,
    val deviceId: String,
    val packageName: String,
    val request: NetworkRequest,
    val response: NetworkResponse? = null,
    val startTime: Long = System.currentTimeMillis(),
    val duration: Long? = null,
)

/**
 * Network request details.
 */
data class NetworkRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String?,
    val contentType: String?,
    val size: Long?,
)

/**
 * Network response details.
 */
data class NetworkResponse(
    val statusCode: Int,
    val statusMessage: String?,
    val headers: Map<String, String>,
    val body: String?,
    val contentType: String?,
    val size: Long?,
    val error: String?,
)

/**
 * Filter criteria for network calls.
 */
data class NetworkFilter(
    val searchText: String = "",
    val methodFilter: String? = null,  // null = all methods
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val deviceFilter: String? = null,  // null = all devices
)

enum class StatusFilter {
    ALL,
    SUCCESS,  // 2xx
    REDIRECT, // 3xx
    CLIENT_ERROR, // 4xx
    SERVER_ERROR, // 5xx
    ERROR, // Connection errors
}
