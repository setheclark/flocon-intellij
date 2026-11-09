package io.github.setheclark.intellij.settings

/**
 * Configuration settings for the network call list.
 */
data class NetworkStorageSettings(
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

    /**
     * Whether network history is written to the history database. When false, history is memory-only.
     * Read once at startup, so a change applies after the IDE restarts.
     */
    val persistHistory: Boolean = true,

    /**
     * Largest the history database may grow, in megabytes. The oldest calls are evicted beyond it.
     */
    val maxDatabaseSizeMb: Int = DEFAULT_MAX_DATABASE_SIZE_MB,

    /**
     * Calls received longer ago than this many days are purged.
     */
    val maxAgeDays: Int = DEFAULT_MAX_AGE_DAYS,
) {
    val maxDatabaseSizeBytes: Long get() = maxDatabaseSizeMb.toLong() * BYTES_PER_MB

    val maxAgeMillis: Long get() = maxAgeDays.toLong() * MILLIS_PER_DAY

    companion object {
        const val DEFAULT_MAX_DATABASE_SIZE_MB = 1024
        const val DEFAULT_MAX_AGE_DAYS = 7

        private const val BYTES_PER_MB = 1024L * 1024L
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
