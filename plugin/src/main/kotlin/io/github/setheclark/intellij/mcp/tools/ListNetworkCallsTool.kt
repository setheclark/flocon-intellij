package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.*

/**
 * MCP Tool: list_network_calls
 *
 * Lists network calls with optional filtering by device, package, method, URL pattern, etc.
 */
fun createListNetworkCallsTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "list_network_calls",
        description = "List network calls with optional filters. Returns captured HTTP/GraphQL/gRPC network requests and responses from connected Android devices.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("deviceId") {
                    put("type", "string")
                    put("description", "Filter by device ID (optional). If omitted along with packageName, returns empty list.")
                }
                putJsonObject("packageName") {
                    put("type", "string")
                    put("description", "Filter by Android package name (optional). If omitted along with deviceId, returns empty list.")
                }
                putJsonObject("method") {
                    put("type", "string")
                    put("description", "Filter by HTTP method (GET, POST, PUT, DELETE, etc.) (optional)")
                }
                putJsonObject("urlPattern") {
                    put("type", "string")
                    put("description", "Filter by URL regex pattern (optional)")
                }
                putJsonObject("statusCode") {
                    put("type", "number")
                    put("description", "Filter by HTTP status code (optional)")
                }
                putJsonObject("minDuration") {
                    put("type", "number")
                    put("description", "Filter by minimum duration in milliseconds (optional)")
                }
                putJsonObject("limit") {
                    put("type", "number")
                    put("description", "Maximum number of results to return (default: 100, max: 1000)")
                }
            },
            required = emptyList()
        )
    )
}

/**
 * Handler for the list_network_calls tool.
 */
suspend fun handleListNetworkCallsTool(
    request: CallToolRequest,
    adapter: McpNetworkDataAdapter,
): CallToolResult {
    val args = request.params.arguments

    // Extract parameters
    val deviceId = args?.get("deviceId")?.jsonPrimitive?.content
    val packageName = args?.get("packageName")?.jsonPrimitive?.content
    val method = args?.get("method")?.jsonPrimitive?.content
    val urlPattern = args?.get("urlPattern")?.jsonPrimitive?.content
    val statusCode = args?.get("statusCode")?.jsonPrimitive?.intOrNull
    val minDuration = args?.get("minDuration")?.jsonPrimitive?.doubleOrNull
    val limit = (args?.get("limit")?.jsonPrimitive?.intOrNull ?: 100).coerceIn(1, 1000)

    // Query network calls
    val calls = adapter.listCalls(
        deviceId = deviceId,
        packageName = packageName,
        method = method,
        urlPattern = urlPattern,
        statusCode = statusCode,
        minDuration = minDuration,
        limit = limit
    )

    // Format response
    val responseText = if (calls.isEmpty()) {
        "No network calls found matching the specified criteria."
    } else {
        buildString {
            appendLine("Found ${calls.size} network call(s):\n")
            calls.forEachIndexed { index, call ->
                appendLine("=".repeat(80))
                appendLine("Call #${index + 1}")
                appendLine("=".repeat(80))
                appendLine(formatNetworkCall(call))
                appendLine()
            }
        }
    }

    return CallToolResult(
        content = listOf(TextContent(text = responseText))
    )
}

/**
 * Formats a NetworkCallEntity as a human-readable string.
 */
internal fun formatNetworkCall(call: NetworkCallEntity): String = buildString {
    appendLine("Call ID: ${call.callId}")
    appendLine("Device: ${call.deviceId}")
    appendLine("Package: ${call.packageName}")
    appendLine("Name: ${call.name}")
    appendLine("Start Time: ${call.startTime} (epoch ms)")
    appendLine()

    // Request section
    appendLine("REQUEST:")
    appendLine("  Method: ${call.request.method}")
    appendLine("  URL: ${call.request.url}")
    appendLine("  Type: ${formatRequestType(call.request.type)}")
    appendLine("  Size: ${call.request.size?.let { "$it bytes" } ?: "unknown"}")
    appendLine("  Headers:")
    call.request.headers.forEach { (key, value) ->
        appendLine("    $key: $value")
    }
    if (call.request.body != null) {
        appendLine("  Body:")
        appendLine(call.request.body.prependIndent("    "))
    }
    appendLine()

    // Response section
    when (val response = call.response) {
        is NetworkResponse.Success -> {
            appendLine("RESPONSE: Success")
            appendLine("  Duration: ${response.durationMs} ms")
            appendLine("  Status Code: ${response.statusCode ?: "N/A"}")
            appendLine("  Content Type: ${response.contentType ?: "N/A"}")
            appendLine("  Size: ${response.size?.let { "$it bytes" } ?: "unknown"}")
            appendLine("  Headers:")
            response.headers.forEach { (key, value) ->
                appendLine("    $key: $value")
            }
            if (response.body != null) {
                appendLine("  Body:")
                appendLine(response.body.prependIndent("    "))
            }
        }
        is NetworkResponse.Failure -> {
            appendLine("RESPONSE: Failure")
            appendLine("  Duration: ${response.durationMs} ms")
            appendLine("  Issue: ${response.issue}")
        }
        null -> {
            appendLine("RESPONSE: Pending (no response yet)")
        }
    }
}

/**
 * Formats the request type as a human-readable string.
 */
internal fun formatRequestType(type: NetworkRequest.Type): String = when (type) {
    is NetworkRequest.Type.Http -> "HTTP"
    is NetworkRequest.Type.GraphQl -> "GraphQL (${type.operationType}${type.operationName?.let { ": $it" } ?: ""})"
    is NetworkRequest.Type.Grpc -> "gRPC"
}
