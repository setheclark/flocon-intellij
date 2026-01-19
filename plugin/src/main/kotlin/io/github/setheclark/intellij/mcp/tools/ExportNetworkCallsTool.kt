package io.github.setheclark.intellij.mcp.tools

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.mcp.McpNetworkDataAdapter
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.text.SimpleDateFormat
import java.util.*

// Shared JSON formatter to avoid redundant creation warning
private val prettyJson = Json { prettyPrint = true }

/**
 * MCP Tool: export_network_calls
 *
 * Exports network calls to various formats for use in external tools and documentation.
 * Supports HAR (HTTP Archive) format for compatibility with Chrome DevTools, Fiddler, Charles Proxy, etc.
 */
fun createExportNetworkCallsTool(adapter: McpNetworkDataAdapter): Tool {
    return Tool(
        name = "export_network_calls",
        description = """Export network calls to various formats.

Supported formats:
- **har**: HTTP Archive format (v1.2) - compatible with Chrome DevTools, Fiddler, Charles Proxy
- **json**: Simple JSON array of calls with full details
- **markdown**: Human-readable Markdown documentation

Use cases:
- Import into Chrome DevTools for visual analysis
- Share with team for debugging
- Generate documentation
- Archive for regression testing""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("callIds") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                    put("description", "Array of call IDs to export (required)")
                }
                putJsonObject("format") {
                    put("type", "string")
                    put("description", "Export format: 'har', 'json', or 'markdown' (default: 'har')")
                    putJsonArray("enum") {
                        add("har")
                        add("json")
                        add("markdown")
                    }
                }
            },
            required = listOf("callIds")
        )
    )
}

/**
 * Handler for the export_network_calls tool.
 */
suspend fun handleExportNetworkCallsTool(
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

    val format = args.get("format")?.jsonPrimitive?.content ?: "har"
    if (format !in listOf("har", "json", "markdown")) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: Invalid format '$format'. Must be 'har', 'json', or 'markdown'.")
            )
        )
    }

    // Fetch all calls
    val calls = callIds.mapNotNull { adapter.getCallById(it) }

    if (calls.isEmpty()) {
        return CallToolResult(
            content = listOf(
                TextContent(text = "Error: None of the specified call IDs were found. They may have been evicted from the cache.")
            )
        )
    }

    // Generate export
    val exportText = when (format) {
        "har" -> exportToHAR(calls)
        "json" -> exportToJSON(calls)
        "markdown" -> exportToMarkdown(calls)
        else -> "Invalid format"
    }

    return CallToolResult(
        content = listOf(TextContent(text = exportText))
    )
}

/**
 * Exports calls to HAR (HTTP Archive) 1.2 format.
 * Spec: http://www.softwareishard.com/blog/har-12-spec/
 */
private fun exportToHAR(calls: List<NetworkCallEntity>): String {
    val har = buildJsonObject {
        putJsonObject("log") {
            put("version", "1.2")
            putJsonObject("creator") {
                put("name", "Flocon Network Inspector")
                put("version", "0.1.0")
            }
            putJsonArray("entries") {
                calls.forEach { call ->
                    add(buildJsonObject {
                        put("startedDateTime", formatISO8601(call.startTime))
                        put("time", call.response?.durationMs ?: 0.0)
                        putJsonObject("request") {
                            put("method", call.request.method)
                            put("url", call.request.url)
                            put("httpVersion", "HTTP/1.1")
                            putJsonArray("headers") {
                                call.request.headers.forEach { (name, value) ->
                                    add(buildJsonObject {
                                        put("name", name)
                                        put("value", value)
                                    })
                                }
                            }
                            putJsonArray("queryString") {
                                // Parse query string from URL
                                val url = call.request.url
                                val queryStart = url.indexOf('?')
                                if (queryStart != -1 && queryStart < url.length - 1) {
                                    val queryString = url.substring(queryStart + 1)
                                    queryString.split('&').forEach { param ->
                                        val parts = param.split('=', limit = 2)
                                        if (parts.isNotEmpty()) {
                                            add(buildJsonObject {
                                                put("name", parts[0])
                                                put("value", parts.getOrNull(1) ?: "")
                                            })
                                        }
                                    }
                                }
                            }
                            putJsonObject("postData") {
                                if (call.request.body != null) {
                                    put("mimeType", call.request.headers["Content-Type"] ?: "application/octet-stream")
                                    put("text", call.request.body)
                                }
                            }
                            put("headersSize", -1)
                            put("bodySize", call.request.size ?: -1)
                        }
                        putJsonObject("response") {
                            when (val response = call.response) {
                                is NetworkResponse.Success -> {
                                    put("status", response.statusCode ?: 200)
                                    put("statusText", "OK")
                                    put("httpVersion", "HTTP/1.1")
                                    putJsonArray("headers") {
                                        response.headers.forEach { (name, value) ->
                                            add(buildJsonObject {
                                                put("name", name)
                                                put("value", value)
                                            })
                                        }
                                    }
                                    putJsonObject("content") {
                                        put("size", response.size ?: -1)
                                        put("mimeType", response.contentType ?: "application/octet-stream")
                                        if (response.body != null) {
                                            put("text", response.body)
                                        }
                                    }
                                    put("redirectURL", "")
                                    put("headersSize", -1)
                                    put("bodySize", response.size ?: -1)
                                }

                                is NetworkResponse.Failure -> {
                                    put("status", 0)
                                    put("statusText", response.issue)
                                    put("httpVersion", "HTTP/1.1")
                                    putJsonArray("headers") { }
                                    putJsonObject("content") {
                                        put("size", 0)
                                        put("mimeType", "text/plain")
                                        put("text", response.issue)
                                    }
                                    put("redirectURL", "")
                                    put("headersSize", -1)
                                    put("bodySize", 0)
                                }

                                null -> {
                                    put("status", 0)
                                    put("statusText", "Pending")
                                    put("httpVersion", "HTTP/1.1")
                                    putJsonArray("headers") { }
                                    putJsonObject("content") {
                                        put("size", 0)
                                        put("mimeType", "text/plain")
                                    }
                                    put("redirectURL", "")
                                    put("headersSize", -1)
                                    put("bodySize", 0)
                                }
                            }
                        }
                        putJsonObject("cache") { }
                        putJsonObject("timings") {
                            put("send", -1)
                            put("wait", call.response?.durationMs ?: 0.0)
                            put("receive", -1)
                        }
                    })
                }
            }
        }
    }

    return Json.encodeToString(JsonObject.serializer(), har)
}

/**
 * Exports calls to simple JSON format.
 */
private fun exportToJSON(calls: List<NetworkCallEntity>): String {
    val jsonArray = buildJsonArray {
        calls.forEach { call ->
            add(buildJsonObject {
                put("callId", call.callId)
                put("deviceId", call.deviceId)
                put("packageName", call.packageName)
                put("startTime", call.startTime)
                put("name", call.name)
                putJsonObject("request") {
                    put("method", call.request.method)
                    put("url", call.request.url)
                    put("type", call.request.type.toString())
                    putJsonObject("headers") {
                        call.request.headers.forEach { (k, v) -> put(k, v) }
                    }
                    put("body", call.request.body)
                    put("size", call.request.size)
                }
                when (val response = call.response) {
                    is NetworkResponse.Success -> {
                        putJsonObject("response") {
                            put("status", "success")
                            put("statusCode", response.statusCode)
                            put("durationMs", response.durationMs)
                            putJsonObject("headers") {
                                response.headers.forEach { (k, v) -> put(k, v) }
                            }
                            put("body", response.body)
                            put("contentType", response.contentType)
                            put("size", response.size)
                        }
                    }

                    is NetworkResponse.Failure -> {
                        putJsonObject("response") {
                            put("status", "failure")
                            put("durationMs", response.durationMs)
                            put("issue", response.issue)
                        }
                    }

                    null -> {
                        put("response", JsonNull)
                    }
                }
            })
        }
    }

    return prettyJson.encodeToString(JsonArray.serializer(), jsonArray)
}

/**
 * Exports calls to Markdown documentation format.
 */
private fun exportToMarkdown(calls: List<NetworkCallEntity>): String = buildString {
    appendLine("# Network Calls Export")
    appendLine()
    appendLine("Exported ${calls.size} network call(s) at ${formatISO8601(System.currentTimeMillis())}")
    appendLine()

    calls.forEachIndexed { index, call ->
        appendLine("## ${index + 1}. ${call.request.method} ${call.request.url}")
        appendLine()
        appendLine("- **Call ID:** ${call.callId}")
        appendLine("- **Time:** ${formatISO8601(call.startTime)}")
        appendLine("- **Device:** ${call.deviceId}")
        appendLine("- **Package:** ${call.packageName}")
        appendLine()

        appendLine("### Request")
        appendLine("```http")
        appendLine("${call.request.method} ${call.request.url}")
        call.request.headers.forEach { (name, value) ->
            appendLine("$name: $value")
        }
        if (call.request.body != null) {
            appendLine()
            appendLine(call.request.body)
        }
        appendLine("```")
        appendLine()

        appendLine("### Response")
        when (val response = call.response) {
            is NetworkResponse.Success -> {
                appendLine("```http")
                appendLine("HTTP/1.1 ${response.statusCode ?: 200} OK")
                response.headers.forEach { (name, value) ->
                    appendLine("$name: $value")
                }
                if (response.body != null) {
                    appendLine()
                    appendLine(response.body)
                }
                appendLine("```")
                appendLine()
                appendLine("- **Duration:** ${response.durationMs} ms")
                appendLine("- **Size:** ${response.size ?: "unknown"} bytes")
            }

            is NetworkResponse.Failure -> {
                appendLine("**Failed:** ${response.issue}")
                appendLine()
                appendLine("- **Duration:** ${response.durationMs} ms")
            }

            null -> {
                appendLine("**Pending** (no response yet)")
            }
        }
        appendLine()
        appendLine("---")
        appendLine()
    }
}

/**
 * Formats a timestamp as ISO 8601 string.
 */
private fun formatISO8601(timestamp: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(timestamp))
}
