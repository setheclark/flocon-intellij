package io.github.setheclark.intellij.mcp

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.mcp.tools.createFilterNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.createGetNetworkCallTool
import io.github.setheclark.intellij.mcp.tools.createListNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.handleFilterNetworkCallsTool
import io.github.setheclark.intellij.mcp.tools.handleGetNetworkCallTool
import io.github.setheclark.intellij.mcp.tools.handleListNetworkCallsTool
import io.github.setheclark.intellij.util.withPluginTag
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.sse.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.types.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.types.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents

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
    private val networkRepository: io.github.setheclark.intellij.flocon.network.NetworkRepository,
    private val devicesRepository: DevicesRepository,
) {
    private val log = Logger.withPluginTag("McpServerDelegate")
    private var ktorServer: EmbeddedServer<*, *>? = null
    private var mcpServer: Server? = null
    private val notificationJobs = mutableListOf<Job>()

    private fun getConfig(): io.github.setheclark.intellij.settings.NetworkStorageSettings {
        return io.github.setheclark.intellij.settings.NetworkStorageSettingsState.getInstance().toSettings()
    }

    fun configureServer(): Server {
        val server = Server(
            Implementation(
                name = "mcp-kotlin test server",
                version = "0.1.0",
            ),
            ServerOptions(
                capabilities = ServerCapabilities(
//                    prompts = ServerCapabilities.Prompts(listChanged = true),
//                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true),
                ),
            ),
        )

        registerTools(server)

//        server.addPrompt(
//            name = "Kotlin Developer",
//            description = "Develop small kotlin applications",
//            arguments = listOf(
//                PromptArgument(
//                    name = "Project Name",
//                    description = "Project name for the new project",
//                    required = true,
//                ),
//            ),
//        ) { request ->
//            GetPromptResult(
//                messages = listOf(
//                    PromptMessage(
//                        role = Role.User,
//                        content = TextContent(
//                            "Develop a kotlin project named <name>${request.arguments?.get("Project Name")}</name>",
//                        ),
//                    ),
//                ),
//                description = "Description for ${request.name}",
//            )
//        }

//        // Add a tool
//        server.addTool(
//            name = "kotlin-sdk-tool",
//            description = "A test tool",
//        ) { _ ->
//            CallToolResult(
//                content = listOf(TextContent("Hello, Intellij!")),
//            )
//        }
//
//        // Add a resource
//        server.addResource(
//            uri = "https://search.com/",
//            name = "Code Search",
//            description = "Web search engine",
//            mimeType = "text/html",
//        ) { request ->
//            ReadResourceResult(
//                contents = listOf(
//                    TextResourceContents("Placeholder content for ${request.uri}", request.uri, "text/html"),
//                ),
//            )
//        }

        return server
    }

    fun initialize() {
//        val config = getConfig()
        coroutineScope.launch {
            ktorServer = embeddedServer(CIO, host = "127.0.0.1", port = 3002) {
                installCors()
                mcp {
                    return@mcp configureServer()
                }
            }.start(wait = true)
        }
//        val config = getConfig()
//        if (!config.mcpServerEnabled) {
//            log.i { "MCP server is disabled in configuration" }
//            return
//        }
//
//        val port = config.mcpServerPort
//        coroutineScope.launch {
//            try {
//                log.i { "Starting MCP server on port $port" }
//
//                ktorServer = embeddedServer(Netty, port = port, host = "localhost") {
//                    mcp {
//                        createMcpServer().also { server ->
//                            mcpServer = server
//                            // Start logging new network calls
//                            startNetworkCallLogging()
//                        }
//                    }
//                }.start(wait = false)
//
//                log.i { "MCP server started successfully on http://localhost:$port/mcp" }
//            } catch (e: Exception) {
//                log.e(e) { "Failed to start MCP server" }
//            }
//        }
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

    fun shutdown() {
        try {
            log.i { "Shutting down MCP server" }

            // Cancel all notification subscriptions
            notificationJobs.forEach { it.cancel() }
            notificationJobs.clear()

            ktorServer?.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
            ktorServer = null
            mcpServer = null
            log.i { "MCP server shut down successfully" }
        } catch (e: Exception) {
            log.e(e) { "Error shutting down MCP server" }
        }
    }

    /**
     * Restart the MCP server with current configuration.
     * This allows dynamic configuration changes without restarting the plugin.
     */
    fun restart() {
        log.i { "Restarting MCP server" }
        shutdown()
        initialize()
    }

    private fun createMcpServer(): Server {
        log.i { "Creating MCP server with network debugging tools" }

        val server = Server(
            serverInfo = Implementation(
                name = "flocon-network-inspector", version = "0.1.4"
            ), options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true)
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

    private fun startNetworkCallLogging() {
        log.i { "Starting network call logging for MCP server" }

        // Log new network calls as they arrive for the currently selected device/app
        // Clients can query these via the MCP tools (poll-based access)
        val job = coroutineScope.launch {
            devicesRepository.observeCurrentDevice().flatMapLatest { device ->
                if (device == null) {
                    flowOf(Pair(null, null))
                } else {
                    // Observe the selected app for this device, keeping the device info
                    devicesRepository.observeDeviceSelectedApp(device.deviceId).map { app ->
                        // Map to a pair of deviceId and app
                        Pair(device.deviceId, app)
                    }
                }
            }.catch { e -> log.e(e) { "Error observing current device/app" } }.collect { (deviceId, app) ->
                if (deviceId != null && app != null) {
                    // Cancel any previous subscription job before starting a new one
                    notificationJobs.filterIsInstance<Job>().drop(1).forEach { it.cancel() }

                    subscribeToDeviceNetworkCalls(deviceId, app.packageName)
                } else {
                    log.d { "No device or app selected, skipping network call notifications" }
                }
            }
        }

        notificationJobs.add(job)
        log.i { "Network call logging started" }
    }

    private suspend fun subscribeToDeviceNetworkCalls(deviceId: String, packageName: String?) {
        if (packageName == null) {
            log.d { "No package selected for device $deviceId, skipping network call notifications" }
            return
        }

        log.i { "Subscribing to network calls for device=$deviceId, package=$packageName" }

        // Keep track of call IDs we've already notified about to avoid duplicates
        val seenCallIds = mutableSetOf<String>()

        networkRepository.observeCalls(deviceId, packageName)
            .catch { e -> log.e(e) { "Error observing network calls for $deviceId/$packageName" } }.onEach { calls ->
                // Find new calls that we haven't seen before
                val newCalls = calls.filter { it.callId !in seenCallIds }

                newCalls.forEach { call ->
                    seenCallIds.add(call.callId)
                    logNetworkCall(call)
                }
            }.collect { }
    }

    private fun logNetworkCall(call: io.github.setheclark.intellij.flocon.network.NetworkCallEntity) {
        log.i {
            "New network call: ${call.callId} - ${call.request.method} ${call.request.url}"
        }

        // Network calls are stored in NetworkDataSource and available via MCP tools:
        // - list_network_calls: Get recent network calls (with optional filters)
        // - get_network_call: Get specific call details by ID
        // - filter_network_calls: Advanced filtering by type, time, status, etc.
        //
        // AI clients should poll these tools to discover new network calls.
    }
}
