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
)
