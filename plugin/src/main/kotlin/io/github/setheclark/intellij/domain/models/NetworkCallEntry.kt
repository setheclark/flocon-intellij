package io.github.setheclark.intellij.domain.models

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
