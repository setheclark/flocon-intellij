package io.github.setheclark.intellij.mcp

import dev.zacsweers.metro.Inject

/**
 * Configuration for the MCP (Model Context Protocol) server.
 *
 * Phase 1: Hardcoded defaults
 * Phase 4: Will read from NetworkStorageSettingsState
 */
@Inject
data class McpServerConfig(
    val enabled: Boolean = true,
    val port: Int = 8086,
)
