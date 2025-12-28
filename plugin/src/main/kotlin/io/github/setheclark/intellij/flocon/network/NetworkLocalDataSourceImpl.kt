package io.github.setheclark.intellij.flocon.network

import androidx.paging.PagingData
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.network.datasource.NetworkLocalDataSource
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel
import io.github.openflocon.domain.network.models.NetworkFilterDomainModel
import io.github.openflocon.domain.network.models.NetworkSortDomainModel
import io.github.openflocon.domain.network.models.httpCode
import kotlinx.coroutines.flow.*

/**
 * In-memory implementation of [NetworkLocalDataSource].
 */
@Inject
class NetworkLocalDataSourceImpl : NetworkLocalDataSource {

    /**
     * Key for storing calls by device and package.
     */
    private data class DevicePackageKey(
        val deviceId: String,
        val packageName: String,
    )

    // In-memory storage: DevicePackageKey -> (CallId -> Call)
    private val callsState =
        MutableStateFlow<Map<DevicePackageKey, Map<String, FloconNetworkCallDomainModel>>>(emptyMap())

    private fun DeviceIdAndPackageNameDomainModel.toKey() = DevicePackageKey(deviceId, packageName)

    override suspend fun getRequests(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel,
    ): List<FloconNetworkCallDomainModel> {
        val key = deviceIdAndPackageName.toKey()
        val calls = callsState.value[key]?.values ?: return emptyList()
        return calls
            .filter { call -> matchesFilter(call, filter, deviceIdAndPackageName) }
            .let { filtered -> applySorting(filtered, sortedBy) }
    }

    fun requestsFlow(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel,
    ): Flow<List<FloconNetworkCallDomainModel>> {
        val key = deviceIdAndPackageName.toKey()
        return callsState.map { state ->
            val calls = state[key]?.values ?: emptyList()
            calls
                .filter { call -> matchesFilter(call, filter, deviceIdAndPackageName) }
                .let { filtered -> applySorting(filtered, sortedBy) }
        }
    }

    override fun observeRequests(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        sortedBy: NetworkSortDomainModel?,
        filter: NetworkFilterDomainModel
    ): Flow<PagingData<FloconNetworkCallDomainModel>> {
        // PagingData is complex to implement in-memory; return empty for now
        // The getRequests method provides the core functionality
        return flowOf(PagingData.empty())
    }

    override suspend fun getCalls(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        ids: List<String>
    ): List<FloconNetworkCallDomainModel> {
        val key = deviceIdAndPackageName.toKey()
        val deviceCalls = callsState.value[key] ?: return emptyList()
        return ids.mapNotNull { id -> deviceCalls[id] }
    }

    override suspend fun getCall(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ): FloconNetworkCallDomainModel? {
        val key = deviceIdAndPackageName.toKey()
        return callsState.value[key]?.get(callId)
    }

    override fun observeCall(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ): Flow<FloconNetworkCallDomainModel?> {
        val key = deviceIdAndPackageName.toKey()
        return callsState.map { state ->
            state[key]?.get(callId)
        }
    }

    override suspend fun save(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        call: FloconNetworkCallDomainModel
    ) {
        val key = deviceIdAndPackageName.toKey()
        callsState.update { current ->
            val deviceCalls = current[key] ?: emptyMap()
            val updatedDeviceCalls = deviceCalls + (call.callId to call)
            (current + (key to updatedDeviceCalls)).also {
                println("Call size after update: ${it.size}")
            }
        }
    }

    override suspend fun save(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        calls: List<FloconNetworkCallDomainModel>
    ) {
        if (calls.isEmpty()) return
        val key = deviceIdAndPackageName.toKey()
        callsState.update { current ->
            val deviceCalls = current[key] ?: emptyMap()
            val updatedDeviceCalls = deviceCalls + calls.associateBy { it.callId }
            current + (key to updatedDeviceCalls)
        }
    }

    override suspend fun clearDeviceCalls(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        val key = deviceIdAndPackageName.toKey()
        callsState.update { current ->
            current - key
        }
    }

    override suspend fun deleteRequest(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ) {
        val key = deviceIdAndPackageName.toKey()
        callsState.update { current ->
            val deviceCalls = current[key] ?: return@update current
            val updatedDeviceCalls = deviceCalls - callId
            if (updatedDeviceCalls.isEmpty()) {
                current - key
            } else {
                current + (key to updatedDeviceCalls)
            }
        }
    }

    override suspend fun deleteRequestsBefore(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        callId: String
    ) {
        val key = deviceIdAndPackageName.toKey()
        callsState.update { current ->
            val deviceCalls = current[key] ?: return@update current
            val targetCall = deviceCalls[callId] ?: return@update current
            val targetStartTime = targetCall.request.startTime

            val updatedDeviceCalls = deviceCalls.filterValues { call ->
                call.request.startTime >= targetStartTime
            }

            if (updatedDeviceCalls.isEmpty()) {
                current - key
            } else {
                current + (key to updatedDeviceCalls)
            }
        }
    }

    override suspend fun deleteRequestOnDifferentSession(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel
    ) {
        val key = deviceIdAndPackageName.toKey()
        val currentAppInstance = deviceIdAndPackageName.appInstance
        callsState.update { current ->
            val deviceCalls = current[key] ?: return@update current
            val updatedDeviceCalls = deviceCalls.filterValues { call ->
                call.appInstance == currentAppInstance
            }

            if (updatedDeviceCalls.isEmpty()) {
                current - key
            } else {
                current + (key to updatedDeviceCalls)
            }
        }
    }

    override suspend fun clear() {
        callsState.value = emptyMap()
    }

    // region filtering and sorting

    private fun matchesFilter(
        call: FloconNetworkCallDomainModel,
        filter: NetworkFilterDomainModel,
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel
    ): Boolean {
        // Filter by session if displayOldSessions is false
        if (!filter.displayOldSessions && call.appInstance != deviceIdAndPackageName.appInstance) {
            return false
        }

        // Filter by method if specified
        val methodFilter = filter.methodFilter
        if (!methodFilter.isNullOrEmpty()) {
            if (call.request.method !in methodFilter) {
                return false
            }
        }

        // Filter by text search across all columns
        val filterText = filter.filterOnAllColumns
        if (!filterText.isNullOrBlank()) {
            val searchText = filterText.lowercase()
            val searchableFields = listOf(
                call.request.url,
                call.request.method,
                call.request.domainFormatted,
                call.request.queryFormatted,
                call.request.startTimeFormatted,
                call.response?.statusFormatted.orEmpty(),
                call.response?.durationFormatted.orEmpty(),
            )
            if (searchableFields.none { it.lowercase().contains(searchText) }) {
                return false
            }
        }

        return true
    }

    private fun applySorting(
        calls: Collection<FloconNetworkCallDomainModel>,
        sortedBy: NetworkSortDomainModel?
    ): List<FloconNetworkCallDomainModel> {
        if (sortedBy == null) {
            // Default: sort by start time descending
            return calls.sortedByDescending { it.request.startTime }
        }

        val comparator: Comparator<FloconNetworkCallDomainModel> = when (sortedBy.column) {
            NetworkSortDomainModel.Column.RequestStartTimeFormatted ->
                compareBy { it.request.startTime }

            NetworkSortDomainModel.Column.Method ->
                compareBy { it.request.method }

            NetworkSortDomainModel.Column.Domain ->
                compareBy { it.request.domainFormatted }

            NetworkSortDomainModel.Column.Query ->
                compareBy { it.request.queryFormatted }

            NetworkSortDomainModel.Column.Status ->
                compareBy { it.httpCode() ?: Int.MAX_VALUE }

            NetworkSortDomainModel.Column.Duration ->
                compareBy { it.response?.durationMs ?: Double.MAX_VALUE }
        }

        return if (sortedBy.asc) {
            calls.sortedWith(comparator)
        } else {
            calls.sortedWith(comparator.reversed())
        }
    }

    // endregion
}
