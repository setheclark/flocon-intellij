package io.github.setheclark.intellij.mcp

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.mcp.tools.*
import io.github.setheclark.intellij.util.withPluginTag
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manages the lifecycle of the MCP (Model Context Protocol) server.
 *
 * The MCP server exposes network debugging data to AI agents via SSE (Server-Sent Events).
 * It listens on port 8086 and provides tools for querying network call history and
 * receiving real-time updates.
 *
 * Phase 1: Basic lifecycle management with minimal server
 * Phase 2: Add MCP tools for querying network data
 * Phase 3: Add real-time notifications
 */
@Inject
class McpServerDelegate(
    @param:AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val config: McpServerConfig,
    private val networkDataAdapter: McpNetworkDataAdapter,
) {
    private val log = Logger.withPluginTag("McpServerDelegate")
    private var ktorServer: EmbeddedServer<*, *>? = null

    fun initialize() {
        if (!config.enabled) {
            log.i { "MCP server is disabled in configuration" }
            return
        }

        coroutineScope.launch {
            try {
                log.i { "Starting MCP server on port ${config.port}" }

                ktorServer = embeddedServer(Netty, port = config.port, host = "localhost") {
                    mcp {
                        createMcpServer()
                    }
                }.start(wait = false)

                log.i { "MCP server started successfully on http://localhost:${config.port}/mcp" }
            } catch (e: Exception) {
                log.e(e) { "Failed to start MCP server" }
            }
        }
    }

    fun shutdown() {
        try {
            log.i { "Shutting down MCP server" }
            ktorServer?.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
            ktorServer = null
            log.i { "MCP server shut down successfully" }
        } catch (e: Exception) {
            log.e(e) { "Error shutting down MCP server" }
        }
    }

    private fun createMcpServer(): Server {
        log.i { "Creating MCP server with network debugging tools" }

        val server = Server(
            serverInfo = Implementation(
                name = "flocon-network-inspector",
                version = "0.1.4"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = null)
                )
            )
        )

        // Register network debugging tools
        registerTools(server)

        log.i { "MCP server created with ${server.tools.size} tool(s)" }
        return server
    }

    private fun registerTools(server: Server) {
        // Tool 1: list_network_calls
        server.addTool(createListNetworkCallsTool(networkDataAdapter)) { request ->
            handleListNetworkCallsTool(request, networkDataAdapter)
        }

        // Tool 2: get_network_call
        server.addTool(createGetNetworkCallTool(networkDataAdapter)) { request ->
            handleGetNetworkCallTool(request, networkDataAdapter)
        }

        // Tool 3: filter_network_calls
        server.addTool(createFilterNetworkCallsTool(networkDataAdapter)) { request ->
            handleFilterNetworkCallsTool(request, networkDataAdapter)
        }

        log.i { "Registered 3 MCP tools: list_network_calls, get_network_call, filter_network_calls" }
    }
}
