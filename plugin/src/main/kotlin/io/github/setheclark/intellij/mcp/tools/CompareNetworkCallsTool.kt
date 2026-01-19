package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.*

/**
 * MCP Tool: compare_network_calls
 *
 * Utility tool for side-by-side comparison of two network calls.
 * Useful for debugging (comparing failed vs successful calls) and analysis (detecting differences).
 */
fun createCompareNetworkCallsTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "compare_network_calls",
        description = """Compare two network calls side-by-side to identify differences.

Useful for:
- Debugging: Compare a failed call with a successful one
- Analysis: Detect changes between similar requests
- Validation: Verify calls are truly redundant

Returns a Markdown table showing differences in method, URL, headers, bodies, and responses.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("callId1") {
                    put("type", "string")
                    put("description", "First network call ID to compare (required)")
                }
                putJsonObject("callId2") {
                    put("type", "string")
                    put("description", "Second network call ID to compare (required)")
                }
                putJsonObject("includeHeaders") {
                    put("type", "boolean")
                    put("description", "Include headers in comparison (default: true)")
                }
                putJsonObject("includeBodies") {
                    put("type", "boolean")
                    put("description", "Include request/response bodies in comparison (default: true)")
                }
            },
            required = listOf("callId1", "callId2")
        )
    )
}

/**
 * Handler for the compare_network_calls tool.
 */
suspend fun handleCompareNetworkCallsTool(
    request: CallToolRequest,
    adapter: McpNetworkDataAdapter,
): CallToolResult {
    val args = request.params.arguments

    // Extract parameters
    val callId1 = args?.get("callId1")?.jsonPrimitive?.content
    val callId2 = args?.get("callId2")?.jsonPrimitive?.content

    if (callId1 == null || callId2 == null) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: Both 'callId1' and 'callId2' parameters are required.")
            )
        )
    }

    if (callId1 == callId2) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: Cannot compare a call with itself. Please provide two different call IDs.")
            )
        )
    }

    val includeHeaders = args.get("includeHeaders")?.jsonPrimitive?.booleanOrNull ?: true
    val includeBodies = args.get("includeBodies")?.jsonPrimitive?.booleanOrNull ?: true

    // Fetch both calls
    val call1 = adapter.getCallById(callId1)
    val call2 = adapter.getCallById(callId2)

    if (call1 == null || call2 == null) {
        val missing = listOfNotNull(
            if (call1 == null) callId1 else null,
            if (call2 == null) callId2 else null
        )
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: Call(s) not found: ${missing.joinToString(", ")}. They may have been evicted from the cache.")
            )
        )
    }

    // Generate comparison
    val responseText = compareNetworkCalls(call1, call2, includeHeaders, includeBodies)

    return CallToolResult(
        content = listOf(TextContent(text = responseText))
    )
}

/**
 * Generates a side-by-side comparison of two network calls in Markdown format.
 */
private fun compareNetworkCalls(
    call1: NetworkCallEntity,
    call2: NetworkCallEntity,
    includeHeaders: Boolean,
    includeBodies: Boolean
): String = buildString {
    appendLine("# Network Calls Comparison")
    appendLine()
    appendLine("| Field | Call 1 (${call1.callId.take(8)}) | Call 2 (${call2.callId.take(8)}) | Match |")
    appendLine("|-------|---------|---------|-------|")

    // Basic metadata
    appendLine(compareField("Device", call1.deviceId, call2.deviceId))
    appendLine(compareField("Package", call1.packageName, call2.packageName))
    appendLine(compareField("Start Time", call1.startTime.toString(), call2.startTime.toString()))
    appendLine()

    appendLine("## Request Comparison")
    appendLine()
    appendLine("| Field | Call 1 | Call 2 | Match |")
    appendLine("|-------|--------|--------|-------|")
    appendLine(compareField("Method", call1.request.method, call2.request.method))
    appendLine(compareField("URL", call1.request.url, call2.request.url))
    appendLine(compareField("Type", call1.request.type.toString(), call2.request.type.toString()))
    appendLine(compareField("Size", call1.request.size?.toString() ?: "unknown", call2.request.size?.toString() ?: "unknown"))
    appendLine()

    // Headers comparison
    if (includeHeaders) {
        appendLine("## Request Headers")
        appendLine()
        val allHeaders = (call1.request.headers.keys + call2.request.headers.keys).distinct().sorted()
        if (allHeaders.isNotEmpty()) {
            appendLine("| Header | Call 1 | Call 2 | Match |")
            appendLine("|--------|--------|--------|-------|")
            allHeaders.forEach { header ->
                val value1 = call1.request.headers[header] ?: "-"
                val value2 = call2.request.headers[header] ?: "-"
                appendLine(compareField(header, value1, value2))
            }
            appendLine()
        }
    }

    // Request body comparison
    if (includeBodies) {
        appendLine("## Request Bodies")
        appendLine()
        val body1 = call1.request.body
        val body2 = call2.request.body
        if (body1 != null || body2 != null) {
            val match = body1 == body2
            appendLine("**Bodies ${if (match) "match ✓" else "differ ✗"}**")
            appendLine()

            if (!match) {
                appendLine("**Call 1 Body:**")
                appendLine("```")
                appendLine(body1 ?: "(empty)")
                appendLine("```")
                appendLine()

                appendLine("**Call 2 Body:**")
                appendLine("```")
                appendLine(body2 ?: "(empty)")
                appendLine("```")
                appendLine()
            }
        } else {
            appendLine("Both requests have no body ✓")
            appendLine()
        }
    }

    // Response comparison
    appendLine("## Response Comparison")
    appendLine()
    appendLine("| Field | Call 1 | Call 2 | Match |")
    appendLine("|-------|--------|--------|-------|")

    val status1 = when (call1.response) {
        is NetworkResponse.Success -> "Success"
        is NetworkResponse.Failure -> "Failure"
        null -> "Pending"
    }
    val status2 = when (call2.response) {
        is NetworkResponse.Success -> "Success"
        is NetworkResponse.Failure -> "Failure"
        null -> "Pending"
    }
    appendLine(compareField("Status", status1, status2))

    if (call1.response is NetworkResponse.Success && call2.response is NetworkResponse.Success) {
        val success1 = call1.response
        val success2 = call2.response

        appendLine(compareField("Status Code", success1.statusCode?.toString() ?: "N/A", success2.statusCode?.toString() ?: "N/A"))
        appendLine(compareField("Duration (ms)", success1.durationMs.toString(), success2.durationMs.toString()))
        appendLine(compareField("Content Type", success1.contentType ?: "N/A", success2.contentType ?: "N/A"))
        appendLine(compareField("Size", success1.size?.toString() ?: "unknown", success2.size?.toString() ?: "unknown"))
        appendLine()

        // Response headers
        if (includeHeaders) {
            appendLine("## Response Headers")
            appendLine()
            val allRespHeaders = (success1.headers.keys + success2.headers.keys).distinct().sorted()
            if (allRespHeaders.isNotEmpty()) {
                appendLine("| Header | Call 1 | Call 2 | Match |")
                appendLine("|--------|--------|--------|-------|")
                allRespHeaders.forEach { header ->
                    val value1 = success1.headers[header] ?: "-"
                    val value2 = success2.headers[header] ?: "-"
                    appendLine(compareField(header, value1, value2))
                }
                appendLine()
            }
        }

        // Response body comparison
        if (includeBodies) {
            appendLine("## Response Bodies")
            appendLine()
            val respBody1 = success1.body
            val respBody2 = success2.body
            if (respBody1 != null || respBody2 != null) {
                val match = respBody1 == respBody2
                appendLine("**Bodies ${if (match) "match ✓" else "differ ✗"}**")
                appendLine()

                if (!match) {
                    appendLine("**Call 1 Body:**")
                    appendLine("```")
                    appendLine(respBody1 ?: "(empty)")
                    appendLine("```")
                    appendLine()

                    appendLine("**Call 2 Body:**")
                    appendLine("```")
                    appendLine(respBody2 ?: "(empty)")
                    appendLine("```")
                    appendLine()
                }
            } else {
                appendLine("Both responses have no body ✓")
                appendLine()
            }
        }
    } else if (call1.response is NetworkResponse.Failure || call2.response is NetworkResponse.Failure) {
        val failure1 = call1.response as? NetworkResponse.Failure
        val failure2 = call2.response as? NetworkResponse.Failure

        if (failure1 != null) {
            appendLine(compareField("Duration (ms)", failure1.durationMs.toString(), call2.response?.durationMs?.toString() ?: "N/A"))
            appendLine(compareField("Issue (Call 1)", failure1.issue, failure2?.issue ?: "N/A"))
        }
        if (failure2 != null && failure1 == null) {
            appendLine(compareField("Duration (ms)", call1.response?.durationMs?.toString() ?: "N/A", failure2.durationMs.toString()))
            appendLine(compareField("Issue (Call 2)", "N/A", failure2.issue))
        }
        appendLine()
    }

    // Summary
    appendLine("## Summary")
    appendLine()
    val differences = mutableListOf<String>()

    if (call1.request.method != call2.request.method) differences.add("Method differs")
    if (call1.request.url != call2.request.url) differences.add("URL differs")
    if (call1.request.body != call2.request.body) differences.add("Request body differs")
    if (status1 != status2) differences.add("Response status differs")

    if (differences.isEmpty()) {
        appendLine("**Calls appear to be identical** ✓")
    } else {
        appendLine("**Key Differences:**")
        differences.forEach { diff ->
            appendLine("- $diff")
        }
    }
}

/**
 * Helper to create a comparison row in a Markdown table.
 */
private fun compareField(field: String, value1: String, value2: String): String {
    val match = if (value1 == value2) "✓" else "✗"
    val truncate = { v: String -> if (v.length > 50) v.take(47) + "..." else v }
    return "| $field | ${truncate(value1)} | ${truncate(value2)} | $match |"
}
