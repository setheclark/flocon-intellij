package io.github.setheclark.intellij.mcp

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.network.NetworkDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Adapter that bridges NetworkDataSource with MCP tools.
 * Provides querying, filtering, and data access methods for MCP tools.
 */
@Inject
class McpNetworkDataAdapter(
    private val networkDataSource: NetworkDataSource,
    private val devicesRepository: DevicesRepository,
) {
    /**
     * Get a specific network call by its ID.
     */
    suspend fun getCallById(callId: String): NetworkCallEntity? {
        return networkDataSource.getByCallId(callId)
    }

    /**
     * Get all network calls for a device/package combination with optional filters.
     */
    suspend fun listCalls(
        deviceId: String?,
        packageName: String?,
        method: String? = null,
        urlPattern: String? = null,
        statusCode: Int? = null,
        minDuration: Double? = null,
        limit: Int = 100,
    ): List<NetworkCallEntity> {
        // Apply filters
        return calls(deviceId, packageName)
            .asSequence()
            .filter { call ->
                if (method != null && call.request.method != method) return@filter false
                if (urlPattern != null && !call.request.url.contains(Regex(urlPattern))) return@filter false
                if (statusCode != null) {
                    val responseStatusCode = (call.response as? NetworkResponse.Success)?.statusCode
                    if (responseStatusCode != statusCode) return@filter false
                }
                if (minDuration != null) {
                    val duration = when (val response = call.response) {
                        is NetworkResponse.Success -> response.durationMs
                        is NetworkResponse.Failure -> response.durationMs
                        null -> return@filter false
                    }
                    if (duration < minDuration) return@filter false
                }
                true
            }
            .take(limit)
            .toList()
    }

    /**
     * Filter network calls with advanced criteria.
     */
    suspend fun filterCalls(
        deviceId: String?,
        packageName: String?,
        requestType: NetworkRequest.Type? = null,
        graphQlOperationType: String? = null,
        graphQlOperationName: String? = null,
        hasFailure: Boolean? = null,
        startTimeAfter: Long? = null,
        startTimeBefore: Long? = null,
        limit: Int = 100,
    ): List<NetworkCallEntity> {
        // Apply filters
        return calls(deviceId, packageName)
            .asSequence()
            .filter { call ->
                // Type filtering with instanceof check
                if (requestType != null) {
                    val matches = when (requestType) {
                        is NetworkRequest.Type.Http -> call.request.type is NetworkRequest.Type.Http
                        is NetworkRequest.Type.Grpc -> call.request.type is NetworkRequest.Type.Grpc
                        else -> false
                    }
                    if (!matches) return@filter false
                }

                // GraphQL-specific filters
                if (graphQlOperationType != null || graphQlOperationName != null) {
                    // Only apply GraphQL filters if this is a GraphQL request
                    val graphQlType = call.request.type as? NetworkRequest.Type.GraphQl ?: return@filter false

                    if (graphQlOperationType != null && graphQlType.operationType != graphQlOperationType) {
                        return@filter false
                    }
                    if (graphQlOperationName != null && graphQlType.operationName != graphQlOperationName) {
                        return@filter false
                    }
                }

                if (hasFailure != null) {
                    val isFailure = call.response is NetworkResponse.Failure
                    if (isFailure != hasFailure) return@filter false
                }

                if (startTimeAfter != null && call.startTime < startTimeAfter) return@filter false
                if (startTimeBefore != null && call.startTime > startTimeBefore) return@filter false

                true
            }
            .take(limit)
            .toList()
    }

    /**
     * Observe network calls for a device/package combination.
     * Used for real-time notifications in Phase 3.
     */
    fun observeCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>> {
        return networkDataSource.observeByDeviceAndPackage(deviceId, packageName)
    }

    private suspend fun calls(
        deviceId: String?,
        packageName: String?,
    ): List<NetworkCallEntity> {
        // Get base list from data source
        return when {
            deviceId != null && packageName != null -> networkDataSource.getByDeviceAndPackage(deviceId, packageName)
            deviceId != null -> devicesRepository.getDeviceSelectedApp(deviceId)?.let { app ->
                networkDataSource.getByDeviceAndPackage(deviceId, app.packageName)
            }

            else -> devicesRepository.getCurrentDevice()?.let { device ->
                val app = devicesRepository.getDeviceSelectedApp(device.deviceId)
                if (app != null) {
                    networkDataSource.getByDeviceAndPackage(device.deviceId, app.packageName)
                } else emptyList()
            }
        } ?: emptyList()
    }
}
