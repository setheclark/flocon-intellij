package io.github.setheclark.intellij.domain.models

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
