package io.github.setheclark.intellij.services

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
import kotlinx.coroutines.launch

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
    val connectedDevices: StateFlow<Set<ConnectedDevice>> = appService.connectedDevices

    init {
        thisLogger().info("FloconProjectService initialized for project: ${project.name}")
        // Auto-start server when first project service is created
        appService.startServer()

        // Subscribe to network events from the application service
        subscribeToNetworkEvents()
    }

    private fun subscribeToNetworkEvents() {
        thisLogger().info(">>> Subscribing to network events")
        // Handle incoming network requests
        scope.launch {
            thisLogger().info(">>> Started collecting networkRequests")
            appService.networkRequests.collect { event ->
                thisLogger().info(">>> Received network request in ProjectService: ${event.request.method} ${event.request.url}")
                val entry = NetworkCallEntry(
                    id = event.callId,
                    deviceId = event.deviceId,
                    packageName = event.packageName,
                    request = NetworkRequest(
                        url = event.request.url ?: "",
                        method = event.request.method ?: "UNKNOWN",
                        headers = event.request.requestHeaders ?: emptyMap(),
                        body = event.request.requestBody,
                        contentType = event.request.requestHeaders?.get("Content-Type"),
                        size = event.request.requestSize,
                    ),
                    startTime = event.request.startTime ?: System.currentTimeMillis(),
                )
                addNetworkCall(entry)
            }
        }

        // Handle incoming network responses
        scope.launch {
            thisLogger().info(">>> Started collecting networkResponses")
            appService.networkResponses.collect { event ->
                thisLogger().info(">>> Received network response in ProjectService: ${event.response.responseHttpCode} (${event.response.durationMs}ms)")
                updateNetworkCall(event.callId) { call ->
                    call.copy(
                        response = NetworkResponse(
                            statusCode = event.response.responseHttpCode ?: 0,
                            statusMessage = null,
                            headers = event.response.responseHeaders ?: emptyMap(),
                            body = event.response.responseBody,
                            contentType = event.response.responseContentType,
                            size = event.response.responseSize,
                            error = event.response.responseError,
                        ),
                        duration = event.response.durationMs?.toLong(),
                    )
                }
            }
        }
    }

    /**
     * Add a new network call entry.
     * Called when a request is received from a connected device.
     */
    fun addNetworkCall(call: NetworkCallEntry) {
        _networkCalls.value = _networkCalls.value + call
        thisLogger().info(">>> Added network call: ${call.request.method} ${call.request.url}, total calls: ${_networkCalls.value.size}")
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
