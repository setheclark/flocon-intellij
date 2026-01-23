package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * MCP Tool: get_network_calls
 *
 * Consolidated tool for querying network calls with comprehensive filtering.
 * Replaces list_network_calls and filter_network_calls.
 *
 * Design follows Block's MCP playbook: consolidate related operations into single tool,
 * return Markdown tables (LLM-friendly), separate summary vs detailed views.
 */
fun createGetNetworkCallsTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "get_network_calls",
        description = """Query network calls from connected Android devices with comprehensive filtering.

Returns HTTP/GraphQL/gRPC requests and responses in Markdown format optimized for LLM analysis.

Use 'summary' format (default) for browsing and pattern detection. Use 'detailed' format when you need full metadata but not bodies. Use get_network_call_details for request/response bodies.

Common workflows:
- Find failures: hasFailure=true
- Find slow requests: minDuration=1000
- Time-based analysis: startTimeAfter/Before for clustering
- GraphQL operations: graphQlOperationType, graphQlOperationName
- Pattern detection: urlPattern (regex)""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("deviceId") {
                    put("type", "string")
                    put("description", "Filter by device ID (optional, uses current device if omitted)")
                }
                putJsonObject("packageName") {
                    put("type", "string")
                    put("description", "Filter by Android package name (optional, uses current app if omitted)")
                }
                putJsonObject("method") {
                    put("type", "string")
                    put("description", "Filter by HTTP method: GET, POST, PUT, DELETE, etc. (optional)")
                }
                putJsonObject("urlPattern") {
                    put("type", "string")
                    put("description", "Filter by URL regex pattern (optional)")
                }
                putJsonObject("requestType") {
                    put("type", "string")
                    put("description", "Filter by request type: 'Http', 'GraphQl', or 'Grpc' (optional)")
                }
                putJsonObject("graphQlOperationType") {
                    put("type", "string")
                    put("description", "Filter GraphQL by operation type: 'query', 'mutation', or 'subscription' (optional)")
                }
                putJsonObject("graphQlOperationName") {
                    put("type", "string")
                    put("description", "Filter GraphQL by operation name (optional)")
                }
                putJsonObject("hasFailure") {
                    put("type", "boolean")
                    put("description", "Filter by failure status: true for failed requests, false for successful (optional)")
                }
                putJsonObject("statusCode") {
                    put("type", "number")
                    put("description", "Filter by HTTP status code (optional)")
                }
                putJsonObject("minDuration") {
                    put("type", "number")
                    put("description", "Filter by minimum duration in milliseconds (optional)")
                }
                putJsonObject("startTimeAfter") {
                    put("type", "number")
                    put("description", "Filter calls that started after this timestamp in epoch milliseconds (optional)")
                }
                putJsonObject("startTimeBefore") {
                    put("type", "number")
                    put("description", "Filter calls that started before this timestamp in epoch milliseconds (optional)")
                }
                putJsonObject("limit") {
                    put("type", "number")
                    put("description", "Maximum number of results to return (default: 100, max: 1000)")
                }
                putJsonObject("format") {
                    put("type", "string")
                    put("description", "Output format: 'summary' (default, Markdown table with key fields) or 'detailed' (full metadata without bodies)")
                    putJsonArray("enum") {
                        add("summary")
                        add("detailed")
                    }
                }
            },
            required = emptyList()
        )
    )
}

/**
 * Handler for the get_network_calls tool.
 */
suspend fun handleGetNetworkCallsTool(
    request: CallToolRequest,
    adapter: McpNetworkDataAdapter,
): CallToolResult {
    val args = request.params.arguments

    // Extract parameters
    val deviceId = args?.get("deviceId")?.jsonPrimitive?.content
    val packageName = args?.get("packageName")?.jsonPrimitive?.content
    val method = args?.get("method")?.jsonPrimitive?.content
    val urlPattern = args?.get("urlPattern")?.jsonPrimitive?.content
    val requestTypeStr = args?.get("requestType")?.jsonPrimitive?.content
    val graphQlOperationType = args?.get("graphQlOperationType")?.jsonPrimitive?.content
    val graphQlOperationName = args?.get("graphQlOperationName")?.jsonPrimitive?.content
    val hasFailure = args?.get("hasFailure")?.jsonPrimitive?.booleanOrNull
    val statusCode = args?.get("statusCode")?.jsonPrimitive?.intOrNull
    val minDuration = args?.get("minDuration")?.jsonPrimitive?.doubleOrNull
    val startTimeAfter = args?.get("startTimeAfter")?.jsonPrimitive?.longOrNull
    val startTimeBefore = args?.get("startTimeBefore")?.jsonPrimitive?.longOrNull
    val limit = (args?.get("limit")?.jsonPrimitive?.intOrNull ?: 100).coerceIn(1, 1000)
    val format = args?.get("format")?.jsonPrimitive?.content ?: "summary"

    // Validate format
    if (format !in listOf("summary", "detailed")) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: Invalid format '$format'. Must be 'summary' or 'detailed'.")
            )
        )
    }

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

    // Query network calls with combined filters
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
    ).filter { call ->
        // Additional filters from list_network_calls
        if (method != null && call.request.method != method) return@filter false
        if (urlPattern != null && !call.request.url.contains(Regex(urlPattern))) return@filter false
        if (statusCode != null) {
            val responseStatusCode = (call.response as? NetworkResponse.Success)?.statusCode
            if (responseStatusCode != statusCode) return@filter false
        }
        if (minDuration != null) {
            val duration = call.response?.durationMs ?: return@filter false
            if (duration < minDuration) return@filter false
        }
        true
    }

    // Format response based on format parameter
    val responseText = when (format) {
        "summary" -> formatSummaryTable(calls)
        "detailed" -> formatDetailedView(calls)
        else -> "Invalid format"
    }

    return CallToolResult(
        content = listOf(TextContent(text = responseText))
    )
}

/**
 * Formats calls as a Markdown table (optimized for LLM analysis).
 */
private fun formatSummaryTable(calls: List<NetworkCallEntity>): String {
    if (calls.isEmpty()) {
        return "No network calls found matching the specified criteria."
    }

    val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    return buildString {
        appendLine("Found ${calls.size} network call(s):\n")
        appendLine("| Call ID | Time | Method | URL | Type | Status | Duration |")
        appendLine("|---------|------|--------|-----|------|--------|----------|")

        calls.forEach { call ->
            val time = dateFormat.format(Date(call.startTime))
            val method = call.request.method
            val url = call.request.url.take(60) + if (call.request.url.length > 60) "..." else ""
            val type = formatRequestTypeShort(call.request.type)
            val status = when (val response = call.response) {
                is NetworkResponse.Success -> response.statusCode?.toString() ?: "OK"
                is NetworkResponse.Failure -> "FAIL"
                null -> "PENDING"
            }
            val duration = call.response?.durationMs?.let { "${it.toInt()}ms" } ?: "-"

            appendLine("| ${call.callId} | $time | $method | $url | $type | $status | $duration |")
        }

        appendLine("\nUse get_network_call_details with specific Call IDs to see full request/response bodies and headers.")
    }
}

/**
 * Formats calls in detailed view (full metadata, no bodies).
 */
private fun formatDetailedView(calls: List<NetworkCallEntity>): String {
    if (calls.isEmpty()) {
        return "No network calls found matching the specified criteria."
    }

    return buildString {
        appendLine("Found ${calls.size} network call(s):\n")
        calls.forEachIndexed { index, call ->
            appendLine("## Call #${index + 1}: ${call.callId}")
            appendLine()
            appendLine("**Metadata:**")
            appendLine("- Device: ${call.deviceId}")
            appendLine("- Package: ${call.packageName}")
            appendLine("- Name: ${call.name}")
            appendLine("- Start Time: ${call.startTime} (epoch ms)")
            appendLine()

            appendLine("**Request:**")
            appendLine("- Method: ${call.request.method}")
            appendLine("- URL: ${call.request.url}")
            appendLine("- Type: ${formatRequestType(call.request.type)}")
            appendLine("- Size: ${call.request.size?.let { "$it bytes" } ?: "unknown"}")
            appendLine()

            appendLine("**Response:**")
            when (val response = call.response) {
                is NetworkResponse.Success -> {
                    appendLine("- Status: Success (${response.statusCode ?: "N/A"})")
                    appendLine("- Duration: ${response.durationMs} ms")
                    appendLine("- Content Type: ${response.contentType ?: "N/A"}")
                    appendLine("- Size: ${response.size?.let { "$it bytes" } ?: "unknown"}")
                }
                is NetworkResponse.Failure -> {
                    appendLine("- Status: Failure")
                    appendLine("- Duration: ${response.durationMs} ms")
                    appendLine("- Issue: ${response.issue}")
                }
                null -> {
                    appendLine("- Status: Pending (no response yet)")
                }
            }
            appendLine()
            appendLine("---")
            appendLine()
        }

        appendLine("Use get_network_call_details to see full request/response bodies and headers.")
    }
}

/**
 * Short format for request type (for table).
 */
private fun formatRequestTypeShort(type: NetworkRequest.Type): String = when (type) {
    is NetworkRequest.Type.Http -> "HTTP"
    is NetworkRequest.Type.GraphQl -> "GraphQL"
    is NetworkRequest.Type.Grpc -> "gRPC"
}

/**
 * Full format for request type.
 */
private fun formatRequestType(type: NetworkRequest.Type): String = when (type) {
    is NetworkRequest.Type.Http -> "HTTP"
    is NetworkRequest.Type.GraphQl -> "GraphQL (${type.operationType}${type.operationName?.let { ": $it" } ?: ""})"
    is NetworkRequest.Type.Grpc -> "gRPC"
}
