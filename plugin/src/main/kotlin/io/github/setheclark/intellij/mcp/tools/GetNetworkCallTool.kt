package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * MCP Tool: get_network_call
 *
 * Retrieves detailed information about a specific network call by its ID.
 */
fun createGetNetworkCallTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "get_network_call",
        description = "Get detailed information about a specific network call by its ID. Returns full request and response details including headers and body.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("callId") {
                    put("type", "string")
                    put("description", "The unique ID of the network call to retrieve (required)")
                }
            },
            required = listOf("callId")
        )
    )
}

/**
 * Handler for the get_network_call tool.
 */
suspend fun handleGetNetworkCallTool(
    request: CallToolRequest,
    adapter: McpNetworkDataAdapter,
): CallToolResult {
    val args = request.params.arguments

    // Extract callId parameter
    val callId = args?.get("callId")?.jsonPrimitive?.content
    if (callId == null) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: The 'callId' parameter is required.")
            )
        )
    }

    // Query network call
    val call = adapter.getCallById(callId)
    if (call == null) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: Network call with ID '$callId' not found. The call may have been evicted from the cache or never existed.")
            )
        )
    }

    // Format response (reuse the formatting function from ListNetworkCallsTool)
    val responseText = buildString {
        appendLine("Network Call Details")
        appendLine("=".repeat(80))
        appendLine(formatNetworkCall(call))
    }

    return CallToolResult(
        content = listOf(TextContent(text = responseText))
    )
}
