package io.github.setheclark.intellij.mcp

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.mcp.prompts.createAnalyzeApiHealthPrompt
import io.github.setheclark.intellij.mcp.prompts.createDebugApiFailurePrompt
import io.github.setheclark.intellij.mcp.prompts.createFindRedundantApisPrompt
import io.github.setheclark.intellij.mcp.prompts.createGenerateTestMocksPrompt
import io.github.setheclark.intellij.mcp.prompts.createUnderstandScreenApisPrompt
import io.github.setheclark.intellij.mcp.prompts.handleAnalyzeApiHealthPrompt
import io.github.setheclark.intellij.mcp.prompts.handleDebugApiFailurePrompt
import io.github.setheclark.intellij.mcp.prompts.handleFindRedundantApisPrompt
import io.github.setheclark.intellij.mcp.prompts.handleGenerateTestMocksPrompt
import io.github.setheclark.intellij.mcp.prompts.handleUnderstandScreenApisPrompt
import io.github.setheclark.intellij.mcp.tools.createCompareNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.createExportNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.createGetNetworkCallDetailsTool
import io.github.setheclark.intellij.mcp.tools.createGetNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.handleCompareNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.handleExportNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.handleGetNetworkCallDetailsTool
import io.github.setheclark.intellij.mcp.tools.handleGetNetworkCallsTool
import io.github.setheclark.intellij.settings.PluginSettingsProvider
import io.github.setheclark.intellij.util.withPluginTag
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val networkDataAdapter: McpNetworkDataAdapter,
    private val settingsProvider: PluginSettingsProvider,
) {
    private val log = Logger.withPluginTag("McpServerDelegate")
    private var ktorServer: EmbeddedServer<*, *>? = null

    private var job: Job? = null

    fun initialize() {
        if (job != null) return

        job = coroutineScope.launch {
            settingsProvider.settings
                .map { it.mcpServerEnabled to it.mcpServerPort }
                .distinctUntilChanged()
                .collect { (enabled, port) ->
                    log.i { "MCP settings | enabled: $enabled, port: $port" }
                    shutdownSever()
                    if (enabled) start(port)
                }
        }
    }

    fun shutdown() {
        shutdownSever()
        job?.cancel("Shutting down")
        job = null
    }

    private fun start(port: Int) {
        ktorServer = embeddedServer(Netty, host = "127.0.0.1", port = port) {
            installCors()
            mcp {
                return@mcp configureServer()
            }
        }.start(wait = true)
    }

    private fun shutdownSever() {
        ktorServer ?: return

        try {
            log.i { "Shutting down MCP server" }

            ktorServer?.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
            ktorServer = null
            log.i { "MCP server shut down successfully" }
        } catch (e: Exception) {
            log.e(e) { "Error shutting down MCP server" }
        }
    }

    private fun configureServer(): Server {
        val server = Server(
            Implementation(
                name = "flocon-network-inspector", version = "0.1.4"
            ),
            ServerOptions(
                capabilities = ServerCapabilities(
                    prompts = ServerCapabilities.Prompts(listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true),
                ),
            ),
        )

        registerTools(server)
        registerPrompts(server)

        return server
    }

    private fun Application.installCors() {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            allowNonSimpleContentTypes = true
            anyHost()
        }
    }

    private fun registerTools(server: Server) {
        // Tool 1: get_network_calls (consolidated listing/filtering)
        server.addTool(createGetNetworkCallsTool(networkDataAdapter)) { request ->
            handleGetNetworkCallsTool(request, networkDataAdapter)
        }

        // Tool 2: get_network_call_details (enhanced with batch support)
        server.addTool(createGetNetworkCallDetailsTool(networkDataAdapter)) { request ->
            handleGetNetworkCallDetailsTool(request, networkDataAdapter)
        }

        // Tool 3: compare_network_calls (utility for debugging)
        server.addTool(createCompareNetworkCallsTool(networkDataAdapter)) { request ->
            handleCompareNetworkCallsTool(request, networkDataAdapter)
        }

        // Tool 4: export_network_calls (HAR/JSON/Markdown export)
        server.addTool(createExportNetworkCallsTool(networkDataAdapter)) { request ->
            handleExportNetworkCallsTool(request, networkDataAdapter)
        }

        log.i { "Registered 4 MCP tools: get_network_calls, get_network_call_details, compare_network_calls, export_network_calls" }
    }

    private fun registerPrompts(server: Server) {
        // Prompt 1: find_redundant_apis
        server.addPrompt(createFindRedundantApisPrompt()) { request ->
            handleFindRedundantApisPrompt(request)
        }

        // Prompt 2: debug_api_failure
        server.addPrompt(createDebugApiFailurePrompt()) { request ->
            handleDebugApiFailurePrompt(request)
        }

        // Prompt 3: understand_screen_apis
        server.addPrompt(createUnderstandScreenApisPrompt()) { request ->
            handleUnderstandScreenApisPrompt(request)
        }

        // Prompt 4: generate_test_mocks
        server.addPrompt(createGenerateTestMocksPrompt()) { request ->
            handleGenerateTestMocksPrompt(request)
        }

        // Prompt 5: analyze_api_health
        server.addPrompt(createAnalyzeApiHealthPrompt()) { request ->
            handleAnalyzeApiHealthPrompt(request)
        }

        log.i { "Registered 5 MCP prompts: find_redundant_apis, debug_api_failure, understand_screen_apis, generate_test_mocks, analyze_api_health" }
    }
}
