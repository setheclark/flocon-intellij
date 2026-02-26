package io.github.setheclark.intellij.flocon.network

data class NetworkCallEntity(
    val callId: String,
    val deviceId: String,
    val packageName: String,
    val appInstance: String,
    val startTime: Long,
    val name: String,
    val request: NetworkRequest,
    val response: NetworkResponse?,
)

data class NetworkRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String?,
    val type: Type,
    val size: Long?,
) {
    sealed interface Type {
        data object Http : Type
        data class GraphQl(
            val persisted: Boolean,
            val query: String?,
            val operationName: String?,
            val operationType: String,
        ) : Type

        data object Grpc : Type
    }
}

sealed interface NetworkResponse {
    val durationMs: Double

    data class Success(
        override val durationMs: Double,
        val body: String?,
        val headers: Map<String, String>,
        val size: Long?,
        val contentType: String?,
        val statusCode: Int?,
    ) : NetworkResponse

    data class Failure(
        override val durationMs: Double,
        val issue: String,
    ) : NetworkResponse
}
