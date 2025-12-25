package io.github.setheclark.intellij.ui.list

import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.domain.models.StatusFilter

fun List<NetworkCallEntry>.applyFilter(filter: NetworkFilter): List<NetworkCallEntry> {
    return filter { call ->
        // Text search filter - searches across all fields
        if (filter.searchText.isNotEmpty()) {
            if (!call.matchesSearchText(filter.searchText)) {
                return@filter false
            }
        }

        // Method filter
        if (filter.methodFilter != null) {
            if (!call.request.method.equals(filter.methodFilter, ignoreCase = true)) {
                return@filter false
            }
        }

        // Status filter
        val statusCode = call.response?.statusCode
        val hasError = call.response?.error != null
        when (filter.statusFilter) {
            StatusFilter.ALL -> { /* include all */
            }

            StatusFilter.SUCCESS -> {
                if (statusCode == null || statusCode < 200 || statusCode >= 300) return@filter false
            }

            StatusFilter.REDIRECT -> {
                if (statusCode == null || statusCode < 300 || statusCode >= 400) return@filter false
            }

            StatusFilter.CLIENT_ERROR -> {
                if (statusCode == null || statusCode < 400 || statusCode >= 500) return@filter false
            }

            StatusFilter.SERVER_ERROR -> {
                if (statusCode == null || statusCode < 500 || statusCode >= 600) return@filter false
            }

            StatusFilter.ERROR -> {
                if (!hasError) return@filter false
            }
        }

        // Device filter
        if (filter.deviceFilter != null) {
            if (call.deviceId != filter.deviceFilter) {
                return@filter false
            }
        }

        true
    }
}

/**
 * Check if a network call matches the search text.
 * Searches across URL, method, headers, body, status, and device info.
 */
internal fun NetworkCallEntry.matchesSearchText(searchText: String): Boolean {
    val search = searchText.lowercase()

    // Search in request fields
    if (request.url.lowercase().contains(search)) return true
    if (request.method.lowercase().contains(search)) return true
    if (request.body?.lowercase()?.contains(search) == true) return true
    if (request.contentType?.lowercase()?.contains(search) == true) return true
    if (request.headers.any { (key, value) ->
            key.lowercase().contains(search) || value.lowercase().contains(search)
        }) return true

    // Search in response fields
    response?.let { response ->
        if (response.statusCode.toString().contains(search)) return true
        if (response.statusMessage?.lowercase()?.contains(search) == true) return true
        if (response.body?.lowercase()?.contains(search) == true) return true
        if (response.contentType?.lowercase()?.contains(search) == true) return true
        if (response.error?.lowercase()?.contains(search) == true) return true
        if (response.headers.any { (key, value) ->
                key.lowercase().contains(search) || value.lowercase().contains(search)
            }) return true
    }

    // Search in device/app info
    if (deviceId.lowercase().contains(search)) return true
    if (packageName.lowercase().contains(search)) return true

    return false
}