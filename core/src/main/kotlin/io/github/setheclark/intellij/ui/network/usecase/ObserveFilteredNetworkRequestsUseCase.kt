package io.github.setheclark.intellij.ui.network.usecase

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.domain.models.RequestTypeFilter
import io.github.setheclark.intellij.domain.models.StatusGroup
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRepository
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Inject
class ObserveFilteredNetworkRequestsUseCase(
    private val networkRepository: NetworkRepository,
) {

    operator fun invoke(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel?,
        filter: NetworkFilter,
    ): Flow<List<NetworkCallEntity>> = if (deviceIdAndPackageName == null) {
        flowOf(emptyList())
    } else {
        networkRepository
            .observeCalls(deviceIdAndPackageName.deviceId, deviceIdAndPackageName.packageName)
            .map { it.applyFilter(filter, deviceIdAndPackageName) }
            .distinctUntilChanged()
    }
}

private fun List<NetworkCallEntity>.applyFilter(
    filter: NetworkFilter,
    deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
): List<NetworkCallEntity> = filter { call ->

    // Filter device and app
    if (call.deviceId != deviceIdAndPackageName.deviceId) return@filter false
    if (call.packageName != deviceIdAndPackageName.packageName) return@filter false

    // Text search filter - searches across all fields
    if (filter.searchText.isNotEmpty()) {
        if (!call.matchesSearchText(filter.searchText)) {
            return@filter false
        }
    }

    // Method filter
    if (!filter.methodFilter.isNullOrEmpty()) {
        if (!filter.methodFilter.any { it.equals(call.request.method, ignoreCase = true) }) {
            return@filter false
        }
    }

    // Status filter
    if (!filter.statusFilter.isNullOrEmpty()) {
        val statusCode = (call.response as? NetworkResponse.Success)?.statusCode
        val group = statusCode?.let { code ->
            when (code) {
                in 200..299 -> StatusGroup.S2xx
                in 300..399 -> StatusGroup.S3xx
                in 400..499 -> StatusGroup.S4xx
                in 500..599 -> StatusGroup.S5xx
                else -> null
            }
        }
        if (group == null || group !in filter.statusFilter) {
            return@filter false
        }
    }

    // Type filter
    if (!filter.typeFilter.isNullOrEmpty()) {
        val requestType = when (call.request.type) {
            is NetworkRequest.Type.Http -> RequestTypeFilter.Http
            is NetworkRequest.Type.GraphQl -> RequestTypeFilter.GraphQl
            is NetworkRequest.Type.Grpc -> RequestTypeFilter.Grpc
        }
        if (requestType !in filter.typeFilter) {
            return@filter false
        }
    }

    true
}

internal fun NetworkCallEntity.matchesSearchText(searchText: String): Boolean {
    val search = searchText.lowercase()

    // Search in request fields
    if (request.url.lowercase().contains(search)) return true
    if (request.method.lowercase().contains(search)) return true
    if (request.body?.lowercase()?.contains(search) == true) return true
    if (request.headers.any { (key, value) ->
            key.lowercase().contains(search) || value.lowercase().contains(search)
        }
    ) {
        return true
    }

    // Search in response fields
    response?.let { response ->
        when (response) {
            is NetworkResponse.Failure -> {
                response.issue.lowercase().contains(search)
            }

            is NetworkResponse.Success -> {
                if (response.body?.lowercase()?.contains(search) == true) return true
                if (response.headers.any { (key, value) ->
                        key.lowercase().contains(search) || value.lowercase().contains(search)
                    }
                ) {
                    return true
                }
            }
        }
    }

    return false
}
