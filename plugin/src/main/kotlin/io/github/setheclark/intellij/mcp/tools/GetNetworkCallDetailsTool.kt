package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.*

/**
 * MCP Tool: get_network_call_details
 *
 * Retrieves detailed information for specific network calls including request/response bodies and headers.
 * Supports batch queries for efficiency.
 *
 * Bodies can be large (up to 50MB cache), so they're excluded from get_network_calls and only
 * available through this tool with explicit opt-in.
 */
fun createGetNetworkCallDetailsTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "get_network_call_details",
        description = """Get full details for specific network call(s) including bodies and headers.

Use this tool when you need to inspect request/response bodies, headers, or compare specific calls.
Bodies can be large, so use get_network_calls first to identify relevant calls, then fetch details for those specific IDs.

Supports batch queries: provide multiple callIds to get details for all in a single request.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("callIds") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                    put("description", "Array of network call IDs to retrieve (required). For single call, use array with one element.")
                }
                putJsonObject("includeRequestBody") {
                    put("type", "boolean")
                    put("description", "Include request body in response (default: true)")
                }
                putJsonObject("includeResponseBody") {
                    put("type", "boolean")
                    put("description", "Include response body in response (default: true)")
                }
                putJsonObject("includeHeaders") {
                    put("type", "boolean")
                    put("description", "Include request and response headers (default: true)")
                }
            },
            required = listOf("callIds")
        )
    )
}

/**
 * Handler for the get_network_call_details tool.
 */
suspend fun handleGetNetworkCallDetailsTool(
    request: CallToolRequest,
    adapter: McpNetworkDataAdapter,
): CallToolResult {
    val args = request.params.arguments

    // Extract parameters
    val callIdsJson = args?.get("callIds")?.jsonArray
    if (callIdsJson == null) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: The 'callIds' parameter is required and must be an array of strings.")
            )
        )
    }

    val callIds = callIdsJson.mapNotNull { it.jsonPrimitive.contentOrNull }
    if (callIds.isEmpty()) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: At least one call ID must be provided.")
            )
        )
    }

    val includeRequestBody = args.get("includeRequestBody")?.jsonPrimitive?.booleanOrNull ?: true
    val includeResponseBody = args.get("includeResponseBody")?.jsonPrimitive?.booleanOrNull ?: true
    val includeHeaders = args.get("includeHeaders")?.jsonPrimitive?.booleanOrNull ?: true

    // Fetch all calls
    val calls = callIds.mapNotNull { callId ->
        adapter.getCallById(callId)?.let { callId to it }
    }

    // Check for missing calls
    val missingIds = callIds.filter { callId -> calls.none { it.first == callId } }
    if (missingIds.isNotEmpty()) {
        val missingText = "Warning: The following call IDs were not found: ${missingIds.joinToString(", ")}\n\n"
        if (calls.isEmpty()) {
            return CallToolResult(
                content = listOf(
                    TextContent(text = missingText + "No calls found. They may have been evicted from the cache.")
                )
            )
        }
    }

    // Format response
    val responseText = buildString {
        if (calls.size > 1) {
            appendLine("# Network Call Details (${calls.size} calls)")
            appendLine()
        }

        calls.forEachIndexed { index, (callId, call) ->
            if (calls.size > 1) {
                appendLine("## Call ${index + 1}/${calls.size}: $callId")
                appendLine()
            } else {
                appendLine("# Network Call Details: $callId")
                appendLine()
            }

            appendLine(formatNetworkCallDetails(call, includeRequestBody, includeResponseBody, includeHeaders))

            if (index < calls.size - 1) {
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    return CallToolResult(
        content = listOf(TextContent(text = responseText))
    )
}

/**
 * Formats a NetworkCallEntity with full details in Markdown.
 */
private fun formatNetworkCallDetails(
    call: NetworkCallEntity,
    includeRequestBody: Boolean,
    includeResponseBody: Boolean,
    includeHeaders: Boolean
): String = buildString {
    appendLine("**Metadata:**")
    appendLine("- Call ID: ${call.callId}")
    appendLine("- Device: ${call.deviceId}")
    appendLine("- Package: ${call.packageName}")
    appendLine("- App Instance: ${call.appInstance}")
    appendLine("- Name: ${call.name}")
    appendLine("- Start Time: ${call.startTime} (epoch ms)")
    appendLine()

    // Request section
    appendLine("## Request")
    appendLine()
    appendLine("- **Method:** ${call.request.method}")
    appendLine("- **URL:** ${call.request.url}")
    appendLine("- **Type:** ${formatRequestTypeDetailed(call.request.type)}")
    appendLine("- **Size:** ${call.request.size?.let { "$it bytes" } ?: "unknown"}")
    appendLine()

    if (includeHeaders && call.request.headers.isNotEmpty()) {
        appendLine("**Headers:**")
        appendLine("```")
        call.request.headers.forEach { (key, value) ->
            appendLine("$key: $value")
        }
        appendLine("```")
        appendLine()
    }

    if (includeRequestBody && call.request.body != null) {
        appendLine("**Body:**")
        appendLine("```")
        appendLine(call.request.body)
        appendLine("```")
        appendLine()
    }

    // Response section
    appendLine("## Response")
    appendLine()

    when (val response = call.response) {
        is NetworkResponse.Success -> {
            appendLine("- **Status:** Success")
            appendLine("- **Status Code:** ${response.statusCode ?: "N/A"}")
            appendLine("- **Duration:** ${response.durationMs} ms")
            appendLine("- **Content Type:** ${response.contentType ?: "N/A"}")
            appendLine("- **Size:** ${response.size?.let { "$it bytes" } ?: "unknown"}")
            appendLine()

            if (includeHeaders && response.headers.isNotEmpty()) {
                appendLine("**Headers:**")
                appendLine("```")
                response.headers.forEach { (key, value) ->
                    appendLine("$key: $value")
                }
                appendLine("```")
                appendLine()
            }

            if (includeResponseBody && response.body != null) {
                appendLine("**Body:**")
                appendLine("```")
                appendLine(response.body)
                appendLine("```")
                appendLine()
            }
        }
        is NetworkResponse.Failure -> {
            appendLine("- **Status:** Failure")
            appendLine("- **Duration:** ${response.durationMs} ms")
            appendLine("- **Issue:** ${response.issue}")
            appendLine()
        }
        null -> {
            appendLine("- **Status:** Pending (no response received yet)")
            appendLine()
        }
    }
}

/**
 * Detailed format for request type including GraphQL metadata.
 */
private fun formatRequestTypeDetailed(type: NetworkRequest.Type): String = when (type) {
    is NetworkRequest.Type.Http -> "HTTP"
    is NetworkRequest.Type.GraphQl -> {
        buildString {
            append("GraphQL - ${type.operationType}")
            type.operationName?.let { append(": $it") }
            if (type.persisted) append(" (persisted)")
        }
    }
    is NetworkRequest.Type.Grpc -> "gRPC"
}
