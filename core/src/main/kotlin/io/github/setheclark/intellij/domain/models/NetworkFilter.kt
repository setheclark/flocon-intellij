package io.github.setheclark.intellij.domain.models

/**
 * Filter criteria for network calls.
 */
data class NetworkFilter(
    val searchText: String = "",
    val methodFilter: String? = null, // null = all methods
)
