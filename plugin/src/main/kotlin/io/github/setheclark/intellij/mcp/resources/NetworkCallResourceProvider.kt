package io.github.setheclark.intellij.mcp.resources

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Provides MCP resources for network calls.
 * Each network call is exposed as a resource with URI: flocon://network-calls/{callId}
 */
class NetworkCallResourceProvider(
    private val networkDataAdapter: McpNetworkDataAdapter,
) {
    companion object {
        private const val SCHEME = "flocon"
        private const val RESOURCE_TYPE = "network-calls"

        fun createResourceUri(callId: String): String = "$SCHEME://$RESOURCE_TYPE/$callId"

        fun extractCallId(uri: String): String? {
            val prefix = "$SCHEME://$RESOURCE_TYPE/"
            return if (uri.startsWith(prefix)) uri.substring(prefix.length) else null
        }
    }

    /**
     * List all available network call resources for the specified device/package.
     */
    suspend fun listResources(deviceId: String?, packageName: String?): List<Resource> {
        val calls = networkDataAdapter.listCalls(
            deviceId = deviceId,
            packageName = packageName,
            limit = 1000  // List up to 1000 recent calls
        )

        return calls.map { call ->
            Resource(
                uri = createResourceUri(call.callId),
                name = call.name,
                description = "${call.request.method} ${call.request.url}",
                mimeType = "application/json"
            )
        }
    }

    /**
     * Read a specific network call resource by URI.
     */
    suspend fun readResource(uri: String): ResourceContents? {
        val callId = extractCallId(uri) ?: return null
        val call = networkDataAdapter.getCallById(callId) ?: return null

        return TextResourceContents(
            uri = uri,
            mimeType = "application/json",
            text = formatNetworkCallAsJson(call)
        )
    }

    /**
     * Format a network call as JSON for resource content.
     */
    private fun formatNetworkCallAsJson(call: NetworkCallEntity): String {
        return buildJsonObject {
            put("callId", call.callId)
            put("deviceId", call.deviceId)
            put("packageName", call.packageName)
            put("name", call.name)
            put("startTime", call.startTime)

            put("request", buildJsonObject {
                put("method", call.request.method)
                put("url", call.request.url)
                put("headers", buildJsonObject {
                    call.request.headers.forEach { (key, value) ->
                        put(key, value)
                    }
                })
                call.request.body?.let { put("body", it) }
            })

            when (val response = call.response) {
                is io.github.setheclark.intellij.flocon.network.NetworkResponse.Success -> {
                    put("response", buildJsonObject {
                        put("type", "success")
                        put("statusCode", response.statusCode)
                        put("durationMs", response.durationMs)
                        response.contentType?.let { put("contentType", it) }
                        put("headers", buildJsonObject {
                            response.headers.forEach { (key, value) ->
                                put(key, value)
                            }
                        })
                        response.body?.let { put("body", it) }
                    })
                }
                is io.github.setheclark.intellij.flocon.network.NetworkResponse.Failure -> {
                    put("response", buildJsonObject {
                        put("type", "failure")
                        put("issue", response.issue)
                        put("durationMs", response.durationMs)
                    })
                }
                null -> {
                    put("response", buildJsonObject {
                        put("type", "pending")
                    })
                }
            }
        }.toString()
    }
}
