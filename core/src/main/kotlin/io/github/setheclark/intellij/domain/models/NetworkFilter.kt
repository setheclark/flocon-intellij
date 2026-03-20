package io.github.setheclark.intellij.domain.models

enum class StatusGroup {
    S2xx, S3xx, S4xx, S5xx
}

enum class RequestTypeFilter {
    Http, GraphQl, Grpc
}

/**
 * Filter criteria for network calls.
 */
data class NetworkFilter(
    val searchText: String = "",
    val methodFilter: Set<String>? = null, // null = all methods
    val statusFilter: Set<StatusGroup>? = null, // null = all statuses
    val typeFilter: Set<RequestTypeFilter>? = null, // null = all types
)
