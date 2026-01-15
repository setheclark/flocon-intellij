package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.*

/**
 * MCP Tool: filter_network_calls
 *
 * Advanced filtering of network calls with support for GraphQL operations, time ranges, and failure status.
 */
fun createFilterNetworkCallsTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "filter_network_calls",
        description = "Filter network calls with advanced criteria including request type (Http/GraphQl/Grpc), GraphQL operation details, failure status, and time ranges.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("deviceId") {
                    put("type", "string")
                    put("description", "Filter by device ID (optional)")
                }
                putJsonObject("packageName") {
                    put("type", "string")
                    put("description", "Filter by Android package name (optional)")
                }
                putJsonObject("requestType") {
                    put("type", "string")
                    put("description", "Filter by request type: 'Http', 'GraphQl', or 'Grpc' (optional)")
                }
                putJsonObject("graphQlOperationType") {
                    put("type", "string")
                    put("description", "Filter GraphQL requests by operation type: 'query', 'mutation', or 'subscription' (optional, only applies to GraphQl requests)")
                }
                putJsonObject("graphQlOperationName") {
                    put("type", "string")
                    put("description", "Filter GraphQL requests by operation name (optional, only applies to GraphQl requests)")
                }
                putJsonObject("hasFailure") {
                    put("type", "boolean")
                    put("description", "Filter by failure status: true for failed requests, false for successful requests (optional)")
                }
                putJsonObject("startTimeAfter") {
                    put("type", "number")
                    put("description", "Filter calls that started after this timestamp (epoch milliseconds) (optional)")
                }
                putJsonObject("startTimeBefore") {
                    put("type", "number")
                    put("description", "Filter calls that started before this timestamp (epoch milliseconds) (optional)")
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
 * Handler for the filter_network_calls tool.
 */
suspend fun handleFilterNetworkCallsTool(
    request: CallToolRequest,
    adapter: McpNetworkDataAdapter,
): CallToolResult {
    val args = request.params.arguments

    // Extract parameters
    val deviceId = args?.get("deviceId")?.jsonPrimitive?.content
    val packageName = args?.get("packageName")?.jsonPrimitive?.content
    val requestTypeStr = args?.get("requestType")?.jsonPrimitive?.content
    val graphQlOperationType = args?.get("graphQlOperationType")?.jsonPrimitive?.content
    val graphQlOperationName = args?.get("graphQlOperationName")?.jsonPrimitive?.content
    val hasFailure = args?.get("hasFailure")?.jsonPrimitive?.booleanOrNull
    val startTimeAfter = args?.get("startTimeAfter")?.jsonPrimitive?.longOrNull
    val startTimeBefore = args?.get("startTimeBefore")?.jsonPrimitive?.longOrNull
    val limit = (args?.get("limit")?.jsonPrimitive?.intOrNull ?: 100).coerceIn(1, 1000)

    // Parse request type
    val requestType = requestTypeStr?.let { type ->
        when (type.lowercase()) {
            "http" -> NetworkRequest.Type.Http
            "graphql" -> null // GraphQL will be filtered via graphQl-specific parameters
            "grpc" -> NetworkRequest.Type.Grpc
            else -> {
                return CallToolResult(
                    content = listOf(
                        TextContent(text = "Error: Invalid requestType '$type'. Must be 'Http', 'GraphQl', or 'Grpc'.")
                    )
                )
            }
        }
    }

    // Query network calls with filters
    val calls = adapter.filterCalls(
        deviceId = deviceId,
        packageName = packageName,
        requestType = requestType,
        graphQlOperationType = graphQlOperationType,
        graphQlOperationName = graphQlOperationName,
        hasFailure = hasFailure,
        startTimeAfter = startTimeAfter,
        startTimeBefore = startTimeBefore,
        limit = limit
    )

    // Format response
    val responseText = if (calls.isEmpty()) {
        "No network calls found matching the specified criteria."
    } else {
        buildString {
            appendLine("Found ${calls.size} network call(s) matching filters:\n")
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
