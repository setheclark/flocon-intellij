package io.github.setheclark.intellij.network

import io.github.setheclark.intellij.fakes.FakeNetworkStorageSettingsProvider
import io.github.setheclark.intellij.settings.NetworkStorageSettings
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BodyStore")
class BodyStoreTest {

    private lateinit var settingsProvider: FakeNetworkStorageSettingsProvider
    private lateinit var bodyStore: BodyStore

    private fun createSettings(
        maxStoredCalls: Int = 1000,
        maxBodyCacheSizeBytes: Long = 50L * 1024 * 1024,
        maxBodySizeBytes: Int = 1024 * 1024,
        compressionEnabled: Boolean = true,
    ) = NetworkStorageSettings(
        maxStoredCalls = maxStoredCalls,
        maxBodyCacheSizeBytes = maxBodyCacheSizeBytes,
        maxBodySizeBytes = maxBodySizeBytes,
        compressionEnabled = compressionEnabled,
    )

    @BeforeEach
    fun setup() {
        settingsProvider = FakeNetworkStorageSettingsProvider(createSettings())
        bodyStore = BodyStore(settingsProvider)
    }

    @Nested
    @DisplayName("store()")
    inner class Store {

        @Test
        fun `returns null for null body`() = runTest {
            val key = bodyStore.store("call-1", BodyType.REQUEST, null)
            key.shouldBeNull()
        }

        @Test
        fun `returns null for empty body`() = runTest {
            val key = bodyStore.store("call-1", BodyType.REQUEST, "")
            key.shouldBeNull()
        }

        @Test
        fun `returns key for valid body`() = runTest {
            val key = bodyStore.store("call-1", BodyType.REQUEST, "test body")
            key.shouldNotBeNull()
            key shouldContain "call-1"
            key shouldContain "REQUEST"
        }

        @Test
        fun `generates unique keys for request and response`() = runTest {
            val requestKey = bodyStore.store("call-1", BodyType.REQUEST, "request body")
            val responseKey = bodyStore.store("call-1", BodyType.RESPONSE, "response body")

            requestKey.shouldNotBeNull()
            responseKey.shouldNotBeNull()
            requestKey shouldBe "call-1:REQUEST"
            responseKey shouldBe "call-1:RESPONSE"
        }

        @Test
        fun `stores body that can be retrieved`() = runTest {
            val body = """{"message": "hello world"}"""
            val key = bodyStore.store("call-1", BodyType.REQUEST, body)

            val retrieved = bodyStore.retrieve(key)
            retrieved shouldBe body
        }

        @Test
        fun `updates existing entry with same key`() = runTest {
            val key1 = bodyStore.store("call-1", BodyType.REQUEST, "original body")
            val key2 = bodyStore.store("call-1", BodyType.REQUEST, "updated body")

            key1 shouldBe key2
            bodyStore.retrieve(key1) shouldBe "updated body"
        }
    }

    @Nested
    @DisplayName("compression")
    inner class Compression {

        @Test
        fun `compresses body when compression is enabled`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = true))

            // JSON compresses well
            val largeJson = """{"data": "${"x".repeat(1000)}"}"""
            val key = bodyStore.store("call-1", BodyType.REQUEST, largeJson)

            // Verify it's stored (we can't directly inspect compression, but stats show size)
            val stats = bodyStore.getStats()
            stats.totalSizeBytes shouldBeLessThan largeJson.toByteArray().size.toLong()
        }

        @Test
        fun `stores uncompressed when compression is disabled`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = false))

            val body = """{"data": "${"x".repeat(1000)}"}"""
            val key = bodyStore.store("call-1", BodyType.REQUEST, body)

            val stats = bodyStore.getStats()
            stats.totalSizeBytes shouldBe body.toByteArray().size.toLong()
        }

        @Test
        fun `skips compression when it would increase size`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = true))

            // Very small body - GZIP overhead makes it larger
            val tinyBody = "hi"
            bodyStore.store("call-1", BodyType.REQUEST, tinyBody)

            // Should store uncompressed since compression would increase size
            val stats = bodyStore.getStats()
            stats.totalSizeBytes shouldBe tinyBody.toByteArray().size.toLong()
        }

        @Test
        fun `correctly decompresses compressed bodies`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = true))

            val originalBody = """{"users": [${(1..100).joinToString(",") { """{"id": $it, "name": "User $it"}""" }}]}"""
            val key = bodyStore.store("call-1", BodyType.RESPONSE, originalBody)

            val retrieved = bodyStore.retrieve(key)
            retrieved shouldBe originalBody
        }
    }

    @Nested
    @DisplayName("truncation")
    inner class Truncation {

        @Test
        fun `truncates body exceeding max size`() = runTest {
            val maxSize = 100
            settingsProvider.updateSettings(createSettings(maxBodySizeBytes = maxSize))

            val largeBody = "x".repeat(200)
            val key = bodyStore.store("call-1", BodyType.REQUEST, largeBody)

            val retrieved = bodyStore.retrieve(key)
            retrieved.shouldNotBeNull()
            retrieved shouldContain "x".repeat(maxSize)
            retrieved shouldContain "[Body truncated"
        }

        @Test
        fun `does not truncate body within limit`() = runTest {
            val maxSize = 100
            settingsProvider.updateSettings(createSettings(maxBodySizeBytes = maxSize))

            val body = "x".repeat(50)
            val key = bodyStore.store("call-1", BodyType.REQUEST, body)

            val retrieved = bodyStore.retrieve(key)
            retrieved shouldBe body
        }

        @Test
        fun `does not truncate when max size is 0`() = runTest {
            settingsProvider.updateSettings(createSettings(maxBodySizeBytes = 0))

            val largeBody = "x".repeat(10000)
            val key = bodyStore.store("call-1", BodyType.REQUEST, largeBody)

            val retrieved = bodyStore.retrieve(key)
            retrieved shouldBe largeBody
        }
    }

    @Nested
    @DisplayName("retrieve()")
    inner class Retrieve {

        @Test
        fun `returns null for null key`() = runTest {
            val result = bodyStore.retrieve(null)
            result.shouldBeNull()
        }

        @Test
        fun `returns null for unknown key`() = runTest {
            val result = bodyStore.retrieve("unknown:REQUEST")
            result.shouldBeNull()
        }

        @Test
        fun `returns stored body for valid key`() = runTest {
            val body = "test body content"
            val key = bodyStore.store("call-1", BodyType.REQUEST, body)

            val retrieved = bodyStore.retrieve(key)
            retrieved shouldBe body
        }

        @Test
        fun `updates LRU order on access`() = runTest {
            // Set small cache size to force eviction
            settingsProvider.updateSettings(createSettings(
                maxBodyCacheSizeBytes = 100,
                compressionEnabled = false
            ))

            // Store 3 bodies
            val key1 = bodyStore.store("call-1", BodyType.REQUEST, "a".repeat(40))
            val key2 = bodyStore.store("call-2", BodyType.REQUEST, "b".repeat(40))

            // Access key1 to make it recently used
            bodyStore.retrieve(key1)

            // Store another body that should evict key2 (least recently used)
            bodyStore.store("call-3", BodyType.REQUEST, "c".repeat(40))

            // key1 should still exist, key2 should be evicted
            bodyStore.retrieve(key1).shouldNotBeNull()
            bodyStore.retrieve(key2).shouldBeNull()
        }
    }

    @Nested
    @DisplayName("LRU eviction")
    inner class LruEviction {

        @Test
        fun `evicts oldest entries when cache exceeds max size`() = runTest {
            settingsProvider.updateSettings(createSettings(
                maxBodyCacheSizeBytes = 100,
                compressionEnabled = false
            ))

            // Store entries that exceed cache size
            val key1 = bodyStore.store("call-1", BodyType.REQUEST, "a".repeat(50))
            val key2 = bodyStore.store("call-2", BodyType.REQUEST, "b".repeat(50))
            val key3 = bodyStore.store("call-3", BodyType.REQUEST, "c".repeat(50))

            // First entry should be evicted
            bodyStore.retrieve(key1).shouldBeNull()
            // Later entries should still exist
            bodyStore.retrieve(key2).shouldNotBeNull()
            bodyStore.retrieve(key3).shouldNotBeNull()
        }

        @Test
        fun `respects cache size limit after eviction`() = runTest {
            settingsProvider.updateSettings(createSettings(
                maxBodyCacheSizeBytes = 100,
                compressionEnabled = false
            ))

            // Store several entries
            repeat(10) { i ->
                bodyStore.store("call-$i", BodyType.REQUEST, "x".repeat(30))
            }

            val stats = bodyStore.getStats()
            stats.totalSizeBytes shouldBeLessThan 101
        }
    }

    @Nested
    @DisplayName("remove()")
    inner class Remove {

        @Test
        fun `removes specific body by call ID and type`() = runTest {
            val key = bodyStore.store("call-1", BodyType.REQUEST, "test body")
            bodyStore.retrieve(key).shouldNotBeNull()

            bodyStore.remove("call-1", BodyType.REQUEST)

            bodyStore.retrieve(key).shouldBeNull()
        }

        @Test
        fun `does not affect other bodies for same call`() = runTest {
            val requestKey = bodyStore.store("call-1", BodyType.REQUEST, "request body")
            val responseKey = bodyStore.store("call-1", BodyType.RESPONSE, "response body")

            bodyStore.remove("call-1", BodyType.REQUEST)

            bodyStore.retrieve(requestKey).shouldBeNull()
            bodyStore.retrieve(responseKey).shouldNotBeNull()
        }

        @Test
        fun `updates cache size tracking`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = false))

            bodyStore.store("call-1", BodyType.REQUEST, "test body")
            val statsBefore = bodyStore.getStats()
            statsBefore.totalSizeBytes shouldBeGreaterThan 0

            bodyStore.remove("call-1", BodyType.REQUEST)
            val statsAfter = bodyStore.getStats()
            statsAfter.totalSizeBytes shouldBe 0
        }

        @Test
        fun `handles removal of non-existent entry gracefully`() = runTest {
            // Should not throw
            bodyStore.remove("non-existent", BodyType.REQUEST)

            val stats = bodyStore.getStats()
            stats.entryCount shouldBe 0
        }
    }

    @Nested
    @DisplayName("removeAll()")
    inner class RemoveAll {

        @Test
        fun `removes all bodies for a call ID`() = runTest {
            val requestKey = bodyStore.store("call-1", BodyType.REQUEST, "request body")
            val responseKey = bodyStore.store("call-1", BodyType.RESPONSE, "response body")

            bodyStore.removeAll("call-1")

            bodyStore.retrieve(requestKey).shouldBeNull()
            bodyStore.retrieve(responseKey).shouldBeNull()
        }

        @Test
        fun `does not affect bodies for other call IDs`() = runTest {
            bodyStore.store("call-1", BodyType.REQUEST, "call 1 body")
            val key2 = bodyStore.store("call-2", BodyType.REQUEST, "call 2 body")

            bodyStore.removeAll("call-1")

            bodyStore.retrieve(key2).shouldNotBeNull()
        }

        @Test
        fun `updates cache size tracking correctly`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = false))

            bodyStore.store("call-1", BodyType.REQUEST, "request")
            bodyStore.store("call-1", BodyType.RESPONSE, "response")
            bodyStore.store("call-2", BodyType.REQUEST, "other")

            val sizeBefore = bodyStore.getStats().totalSizeBytes

            bodyStore.removeAll("call-1")

            val sizeAfter = bodyStore.getStats().totalSizeBytes
            sizeAfter shouldBeLessThan sizeBefore
            sizeAfter shouldBe "other".toByteArray().size.toLong()
        }
    }

    @Nested
    @DisplayName("clear()")
    inner class Clear {

        @Test
        fun `removes all entries`() = runTest {
            bodyStore.store("call-1", BodyType.REQUEST, "body 1")
            bodyStore.store("call-2", BodyType.REQUEST, "body 2")
            bodyStore.store("call-3", BodyType.RESPONSE, "body 3")

            bodyStore.clear()

            val stats = bodyStore.getStats()
            stats.entryCount shouldBe 0
            stats.totalSizeBytes shouldBe 0
        }

        @Test
        fun `allows storing new entries after clear`() = runTest {
            bodyStore.store("call-1", BodyType.REQUEST, "original body")
            bodyStore.clear()

            val key = bodyStore.store("call-2", BodyType.REQUEST, "new body")
            bodyStore.retrieve(key) shouldBe "new body"
        }
    }

    @Nested
    @DisplayName("getStats()")
    inner class GetStats {

        @Test
        fun `returns zero stats for empty store`() = runTest {
            val stats = bodyStore.getStats()

            stats.entryCount shouldBe 0
            stats.totalSizeBytes shouldBe 0
        }

        @Test
        fun `returns correct entry count`() = runTest {
            bodyStore.store("call-1", BodyType.REQUEST, "body 1")
            bodyStore.store("call-2", BodyType.REQUEST, "body 2")
            bodyStore.store("call-2", BodyType.RESPONSE, "body 3")

            val stats = bodyStore.getStats()
            stats.entryCount shouldBe 3
        }

        @Test
        fun `returns correct total size for uncompressed entries`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = false))

            val body1 = "body one"
            val body2 = "body two"
            bodyStore.store("call-1", BodyType.REQUEST, body1)
            bodyStore.store("call-2", BodyType.REQUEST, body2)

            val stats = bodyStore.getStats()
            stats.totalSizeBytes shouldBe (body1.toByteArray().size + body2.toByteArray().size).toLong()
        }

        @Test
        fun `returns configured max size`() = runTest {
            val maxSize = 123456L
            settingsProvider.updateSettings(createSettings(maxBodyCacheSizeBytes = maxSize))

            val stats = bodyStore.getStats()
            stats.maxSizeBytes shouldBe maxSize
        }

        @Test
        fun `calculates usage percent correctly`() = runTest {
            settingsProvider.updateSettings(createSettings(
                maxBodyCacheSizeBytes = 100,
                compressionEnabled = false
            ))

            bodyStore.store("call-1", BodyType.REQUEST, "x".repeat(50))

            val stats = bodyStore.getStats()
            stats.usagePercent shouldBe 50
        }
    }

    @Nested
    @DisplayName("concurrency")
    inner class Concurrency {

        @Test
        fun `handles concurrent stores safely`() = runTest {
            settingsProvider.updateSettings(createSettings(compressionEnabled = false))

            // Store many entries concurrently
            kotlinx.coroutines.coroutineScope {
                val jobs = (1..100).map { i ->
                    async {
                        bodyStore.store("call-$i", BodyType.REQUEST, "body $i")
                    }
                }
                jobs.forEach { it.await() }
            }

            val stats = bodyStore.getStats()
            stats.entryCount shouldBe 100
        }

        @Test
        fun `handles concurrent retrieves safely`() = runTest {
            val key = bodyStore.store("call-1", BodyType.REQUEST, "test body")

            // Retrieve concurrently
            kotlinx.coroutines.coroutineScope {
                val results = (1..100).map {
                    async {
                        bodyStore.retrieve(key)
                    }
                }.map { it.await() }

                results.forEach { it shouldBe "test body" }
            }
        }
    }
}
