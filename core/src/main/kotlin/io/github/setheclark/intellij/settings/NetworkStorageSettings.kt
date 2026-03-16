package io.github.setheclark.intellij.settings

/**
 * Configuration settings for network call storage and memory management.
 */
data class NetworkStorageSettings(
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
     * Names of columns that are hidden in the network call list.
     * The TIME column is always visible and is never added to this set.
     */
    val hiddenColumns: Set<String> = emptySet(),

    /**
     * Persisted widths (in dp) for each visible column, keyed by column name.
     * Falls back to the column's [preferredWidth] when no entry is present.
     */
    val columnWidths: Map<String, Float> = emptyMap(),
) {
    companion object {
        const val DEFAULT_MAX_STORED_CALLS = 1000
        const val DEFAULT_MAX_BODY_CACHE_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB
        const val DEFAULT_MAX_BODY_SIZE_BYTES = 1024 * 1024 // 1 MB
        const val DEFAULT_COMPRESSION_ENABLED = true
    }
}
