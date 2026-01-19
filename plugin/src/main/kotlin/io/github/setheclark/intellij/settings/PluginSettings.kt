package io.github.setheclark.intellij.settings

/**
 * Configuration settings for network call storage and memory management.
 */
data class PluginSettings(
    /**
     * Maximum number of network calls to keep in memory.
     * Oldest calls are evicted when this limit is exceeded.
     */
    val maxStoredCalls: Int = DEFAULT_MAX_STORED_CALLS,

    /**
     * Maximum size in bytes for the compressed body cache.
     * Bodies are evicted (LRU) when this limit is exceeded.
     */
    val maxBodyCacheSizeBytes: Long = DEFAULT_MAX_BODY_CACHE_SIZE_BYTES,

    /**
     * Maximum size in bytes for a single body before it's truncated at capture time.
     * Bodies larger than this are truncated with a "[truncated]" indicator.
     * Set to 0 to disable truncation (store full bodies).
     */
    val maxBodySizeBytes: Int = DEFAULT_MAX_BODY_SIZE_BYTES,

    /**
     * Whether to compress bodies in the cache.
     * Uses GZIP compression which typically achieves 80-90% reduction for JSON/XML.
     */
    val compressionEnabled: Boolean = DEFAULT_COMPRESSION_ENABLED,

    /**
     * Whether the MCP (Model Context Protocol) server is enabled.
     * The MCP server exposes network debugging data to AI agents.
     */
    val mcpServerEnabled: Boolean = DEFAULT_MCP_SERVER_ENABLED,

    /**
     * Port number for the MCP server.
     * The server listens on localhost at this port.
     */
    val mcpServerPort: Int = DEFAULT_MCP_SERVER_PORT,
) {
    companion object {
        const val DEFAULT_MAX_STORED_CALLS = 1000
        const val DEFAULT_MAX_BODY_CACHE_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB
        const val DEFAULT_MAX_BODY_SIZE_BYTES = 1024 * 1024 // 1 MB
        const val DEFAULT_COMPRESSION_ENABLED = true
        const val DEFAULT_MCP_SERVER_ENABLED = false
        const val DEFAULT_MCP_SERVER_PORT = 8086

        const val MIN_MCP_SERVER_PORT = 1024
        const val MAX_MCP_SERVER_PORT = 9022
    }
}
