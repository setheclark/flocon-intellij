package io.github.setheclark.intellij.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.settings.PluginSettingsProvider
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Compressed body storage with LRU eviction.
 *
 * Bodies are stored compressed using GZIP to reduce memory footprint.
 * When the cache exceeds the configured size limit, least recently used entries are evicted.
 */
@Inject
@SingleIn(AppScope::class)
class BodyStore(
    private val settingsProvider: PluginSettingsProvider,
) {
    private val log = Logger.withPluginTag("BodyStore")

    private val mutex = Mutex()

    // LinkedHashMap with accessOrder=true provides LRU ordering
    private val cache = object : LinkedHashMap<String, CompressedBody>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CompressedBody>?): Boolean {
            // Don't auto-remove here; we handle eviction manually to track size
            return false
        }
    }

    private var currentSizeBytes: Long = 0

    /**
     * Store a body, compressing it if compression is enabled.
     * Returns the key used to retrieve the body later.
     *
     * @param callId The network call ID
     * @param bodyType Either "request" or "response"
     * @param body The body content to store
     * @return The storage key, or null if the body was empty/null
     */
    suspend fun store(callId: String, bodyType: BodyType, body: String?): String? {
        if (body.isNullOrEmpty()) return null

        val settings = settingsProvider.getSettings()
        val key = makeKey(callId, bodyType)

        // Truncate if body exceeds max size
        val truncatedBody = if (settings.maxBodySizeBytes > 0 && body.length > settings.maxBodySizeBytes) {
            body.take(settings.maxBodySizeBytes) + "\n\n[Body truncated - exceeded ${settings.maxBodySizeBytes} bytes]"
        } else {
            body
        }

        val uncompressedBytes = truncatedBody.toByteArray(Charsets.UTF_8)

        val compressed = if (settings.compressionEnabled) {
            val gzipped = compress(uncompressedBytes)
            // Only use compression if it actually saves space
            if (gzipped.data.size < uncompressedBytes.size) {
                gzipped
            } else {
                CompressedBody(
                    data = uncompressedBytes,
                    originalSize = uncompressedBytes.size,
                    isCompressed = false,
                )
            }
        } else {
            CompressedBody(
                data = uncompressedBytes,
                originalSize = uncompressedBytes.size,
                isCompressed = false,
            )
        }

        mutex.withLock {
            // Remove old entry if exists (to update size tracking)
            cache.remove(key)?.let { old ->
                currentSizeBytes -= old.data.size
            }

            // Add new entry
            cache[key] = compressed
            currentSizeBytes += compressed.data.size

            // Evict LRU entries if over size limit
            evictIfNeeded(settings.maxBodyCacheSizeBytes)
        }

        val originalBytes = uncompressedBytes.size
        val storedBytes = compressed.data.size
        val savingsPercent = if (originalBytes > 0) ((originalBytes - storedBytes) * 100) / originalBytes else 0

        log.d {
            buildString {
                append("Stored $bodyType body: ${formatBytes(originalBytes)} -> ${formatBytes(storedBytes)}")
                if (compressed.isCompressed) {
                    append(" ($savingsPercent% savings)")
                } else if (settings.compressionEnabled) {
                    append(" (compression skipped - would increase size)")
                }
            }
        }
        return key
    }

    /**
     * Retrieve a body by its storage key.
     *
     * @return The decompressed body content, or null if not found (evicted or never stored)
     */
    suspend fun retrieve(key: String?): String? {
        if (key == null) return null

        val compressed = mutex.withLock {
            cache[key] // This also updates LRU order due to accessOrder=true
        } ?: return null

        return decompress(compressed)
    }

    /**
     * Remove a body from the store.
     */
    suspend fun remove(callId: String, bodyType: BodyType) {
        val key = makeKey(callId, bodyType)
        mutex.withLock {
            cache.remove(key)?.let { old ->
                currentSizeBytes -= old.data.size
            }
        }
    }

    /**
     * Remove all bodies for a given call ID.
     */
    suspend fun removeAll(callId: String) {
        mutex.withLock {
            BodyType.entries.forEach { bodyType ->
                val key = makeKey(callId, bodyType)
                cache.remove(key)?.let { old ->
                    currentSizeBytes -= old.data.size
                }
            }
        }
    }

    /**
     * Clear all stored bodies.
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
            currentSizeBytes = 0
        }
    }

    /**
     * Get current cache statistics.
     */
    suspend fun getStats(): BodyStoreStats = mutex.withLock {
        BodyStoreStats(
            entryCount = cache.size,
            totalSizeBytes = currentSizeBytes,
            maxSizeBytes = settingsProvider.getSettings().maxBodyCacheSizeBytes,
        )
    }

    private fun evictIfNeeded(maxSizeBytes: Long) {
        val iterator = cache.entries.iterator()
        while (currentSizeBytes > maxSizeBytes && iterator.hasNext()) {
            val entry = iterator.next()
            currentSizeBytes -= entry.value.data.size
            iterator.remove()
            log.v { "Evicted body ${entry.key} (${entry.value.data.size} bytes)" }
        }
    }

    private fun makeKey(callId: String, bodyType: BodyType): String = "$callId:${bodyType.name}"

    private fun compress(bytes: ByteArray): CompressedBody {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(bytes)
        }
        return CompressedBody(
            data = outputStream.toByteArray(),
            originalSize = bytes.size,
            isCompressed = true,
        )
    }

    private fun decompress(compressed: CompressedBody): String {
        return if (compressed.isCompressed) {
            GZIPInputStream(ByteArrayInputStream(compressed.data)).use { gzip ->
                gzip.readBytes().toString(Charsets.UTF_8)
            }
        } else {
            compressed.data.toString(Charsets.UTF_8)
        }
    }

    private fun formatBytes(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
        }
    }
}

enum class BodyType {
    REQUEST,
    RESPONSE,
}

private data class CompressedBody(
    val data: ByteArray,
    val originalSize: Int,
    val isCompressed: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CompressedBody
        return data.contentEquals(other.data) && originalSize == other.originalSize && isCompressed == other.isCompressed
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + originalSize
        result = 31 * result + isCompressed.hashCode()
        return result
    }
}

data class BodyStoreStats(
    val entryCount: Int,
    val totalSizeBytes: Long,
    val maxSizeBytes: Long,
) {
    val usagePercent: Int get() = if (maxSizeBytes > 0) ((totalSizeBytes * 100) / maxSizeBytes).toInt() else 0
}
