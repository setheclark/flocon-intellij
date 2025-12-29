package io.github.setheclark.intellij.flocon.network

import com.flocon.data.remote.network.mapper.extractGraphQl
import com.flocon.data.remote.network.models.FloconNetworkRequestDataModel
import com.flocon.data.remote.network.models.FloconNetworkResponseDataModel
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.network.graphql.model.GraphQlExtracted
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.setheclark.intellij.util.safeDecode
import kotlinx.serialization.json.Json
import java.net.URI

@Inject
class NetworkMessageMapper(
    private val json: Json,
) {
    fun toResponseModel(message: FloconIncomingMessageDomainModel): FloconNetworkResponseDataModel? =
        json.safeDecode<FloconNetworkResponseDataModel>(message.body)

    fun toCallEntity(
        message: FloconIncomingMessageDomainModel,
    ): NetworkCallEntity? = json.safeDecode<FloconNetworkRequestDataModel>(message.body)?.let { decoded ->
        val graphQl = extractGraphQl(decoded)

        // Bad assumption made here.  If there are call timeline issues, this is likely why.
        val startTime = decoded.startTime ?: System.currentTimeMillis()
        val url = decoded.url!!
        val method = decoded.method!!

        val requestType = when {
            graphQl != null -> {
                when (graphQl) {
                    is GraphQlExtracted.PersistedQuery -> NetworkRequest.Type.GraphQl(
                        persisted = true,
                        query = graphQl.queryName,
                        operationName = graphQl.queryName,
                        operationType = graphQl.operationType
                    )

                    is GraphQlExtracted.WithBody -> NetworkRequest.Type.GraphQl(
                        persisted = false,
                        query = graphQl.requestBody.query,
                        operationName = graphQl.queryName,
                        operationType = graphQl.operationType
                    )
                }
            }

            decoded.floconNetworkType == "grpc" -> NetworkRequest.Type.Grpc
            else -> NetworkRequest.Type.Http
        }

        val name = extractName(requestType, url, method)

        NetworkCallEntity(
            callId = decoded.floconCallId!!,
            deviceId = message.deviceId,
            packageName = message.appPackageName,
            appInstance = message.appInstance.toString(),
            startTime = startTime,
            name = name,
            request = NetworkRequest(
                url = url,
                method = method,
                headers = decoded.requestHeaders!!,
                body = decoded.requestBody,
                size = decoded.requestSize,
                type = requestType,
            ),
            response = null,
        )
    }

    fun updateCallEntity(
        original: NetworkCallEntity,
        responseData: FloconNetworkResponseDataModel,
    ): NetworkCallEntity = with(responseData) {
        val response = if (responseError != null) {
            NetworkResponse.Failure(
                durationMs = durationMs ?: 0.0,
                issue = responseError!!,
            )
        } else {
            NetworkResponse.Success(
                durationMs = durationMs ?: 0.0,
                body = responseBody,
                headers = responseHeaders.orEmpty(),
                size = responseSize,
                contentType = responseContentType,
                statusCode = responseHttpCode,
            )
        }

        return original.copy(response = response)
    }

    private fun extractName(type: NetworkRequest.Type, url: String, method: String): String {
        return when (type) {
            is NetworkRequest.Type.GraphQl -> type.operationName ?: "GraphQL"
            is NetworkRequest.Type.Http -> "$method ${extractUrlPath(url)}"
            is NetworkRequest.Type.Grpc -> extractGrpcMethodName(url)
        }
    }

    private fun extractUrlPath(url: String): String {
        return try {
            URI(url).path ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun extractGrpcMethodName(url: String): String {
        // gRPC URLs typically follow /package.Service/MethodName format
        val path = extractUrlPath(url)
        return path.substringAfterLast('/')
    }
}