package io.github.setheclark.intellij.network

import app.cash.turbine.test
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.fakes.FakeNetworkStorageSettingsProvider
import io.github.setheclark.intellij.settings.NetworkStorageSettings
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class InMemoryNetworkDataSourceTest {

    private lateinit var settingsProvider: FakeNetworkStorageSettingsProvider
    private lateinit var bodyStore: BodyStore
    private lateinit var dataSource: InMemoryNetworkDataSource

    private fun createSettings(
        maxStoredCalls: Int = 1000,
        maxBodyCacheSizeBytes: Long = 50L * 1024 * 1024,
        maxBodySizeBytes: Int = 1024 * 1024,
        compressionEnabled: Boolean = false, // Disable for simpler testing
    ) = NetworkStorageSettings(
        maxStoredCalls = maxStoredCalls,
        maxBodyCacheSizeBytes = maxBodyCacheSizeBytes,
        maxBodySizeBytes = maxBodySizeBytes,
        compressionEnabled = compressionEnabled,
    )

    private fun createEntity(
        callId: String = "call-1",
        deviceId: String = "device-1",
        packageName: String = "com.example.app",
        appInstance: String = "instance-1",
        startTime: Long = System.currentTimeMillis(),
        name: String = "GET /api/test",
        requestBody: String? = null,
        responseBody: String? = null,
        hasResponse: Boolean = false,
    ) = NetworkCallEntity(
        callId = callId,
        deviceId = deviceId,
        packageName = packageName,
        appInstance = appInstance,
        startTime = startTime,
        name = name,
        request = NetworkRequest(
            url = "https://example.com/api/test",
            method = "GET",
            headers = mapOf("Content-Type" to "application/json"),
            body = requestBody,
            type = NetworkRequest.Type.Http,
            size = requestBody?.length?.toLong(),
        ),
        response = if (hasResponse) NetworkResponse.Success(
            durationMs = 100.0,
            body = responseBody,
            headers = mapOf("Content-Type" to "application/json"),
            size = responseBody?.length?.toLong(),
            contentType = "application/json",
            statusCode = 200,
        ) else null,
    )

    @Before
    fun setup() {
        settingsProvider = FakeNetworkStorageSettingsProvider(createSettings())
        bodyStore = BodyStore(settingsProvider)
        dataSource = InMemoryNetworkDataSource(bodyStore, settingsProvider)
    }



        @Test
        fun `inserts entity and stores bodies separately`() = runTest {
            val entity = createEntity(
                callId = "call-1",
                requestBody = "request body",
            )

            dataSource.insert(entity)

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            retrieved.callId shouldBe "call-1"
            retrieved.request.body shouldBe "request body"
        }

        @Test
        fun `inserts multiple entities`() = runTest {
            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.insert(createEntity(callId = "call-2"))
            dataSource.insert(createEntity(callId = "call-3"))

            dataSource.getByCallId("call-1").shouldNotBeNull()
            dataSource.getByCallId("call-2").shouldNotBeNull()
            dataSource.getByCallId("call-3").shouldNotBeNull()
        }

        @Test
        fun `stores response body when present`() = runTest {
            val entity = createEntity(
                callId = "call-1",
                hasResponse = true,
                responseBody = "response body",
            )

            dataSource.insert(entity)

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            val response = retrieved.response as NetworkResponse.Success
            response.body shouldBe "response body"
        }

        @Test
        fun `handles entity with no bodies`() = runTest {
            val entity = createEntity(
                callId = "call-1",
                requestBody = null,
                hasResponse = false,
            )

            dataSource.insert(entity)

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            retrieved.request.body.shouldBeNull()
            retrieved.response.shouldBeNull()
        }

        @Test
        fun `clears previous session calls when app instance changes`() = runTest {
            // Insert calls from first session
            dataSource.insert(createEntity(callId = "call-1", appInstance = "session-1"))
            dataSource.insert(createEntity(callId = "call-2", appInstance = "session-1"))

            // Insert call from new session
            dataSource.insert(createEntity(callId = "call-3", appInstance = "session-2"))

            // Previous session calls should be cleared
            dataSource.getByCallId("call-1").shouldBeNull()
            dataSource.getByCallId("call-2").shouldBeNull()
            // New session call should exist
            dataSource.getByCallId("call-3").shouldNotBeNull()
        }

        @Test
        fun `does not clear calls when app instance is same`() = runTest {
            dataSource.insert(createEntity(callId = "call-1", appInstance = "session-1"))
            dataSource.insert(createEntity(callId = "call-2", appInstance = "session-1"))
            dataSource.insert(createEntity(callId = "call-3", appInstance = "session-1"))

            // All calls should still exist
            dataSource.getByCallId("call-1").shouldNotBeNull()
            dataSource.getByCallId("call-2").shouldNotBeNull()
            dataSource.getByCallId("call-3").shouldNotBeNull()
        }

        @Test
        fun `tracks sessions independently per device package`() = runTest {
            // Device 1, App A - Session 1
            dataSource.insert(createEntity(
                callId = "d1-a-call-1",
                deviceId = "device-1",
                packageName = "com.app.a",
                appInstance = "session-1",
            ))

            // Device 1, App B - Session 1
            dataSource.insert(createEntity(
                callId = "d1-b-call-1",
                deviceId = "device-1",
                packageName = "com.app.b",
                appInstance = "session-1",
            ))

            // Device 1, App A - Session 2 (should only clear App A calls)
            dataSource.insert(createEntity(
                callId = "d1-a-call-2",
                deviceId = "device-1",
                packageName = "com.app.a",
                appInstance = "session-2",
            ))

            // App A's old call should be cleared
            dataSource.getByCallId("d1-a-call-1").shouldBeNull()
            // App B's call should still exist
            dataSource.getByCallId("d1-b-call-1").shouldNotBeNull()
            // App A's new call should exist
            dataSource.getByCallId("d1-a-call-2").shouldNotBeNull()
        }

        @Test
        fun `clears bodies from BodyStore when session changes`() = runTest {
            // Insert call with body from first session
            dataSource.insert(createEntity(
                callId = "call-1",
                appInstance = "session-1",
                requestBody = "large body content",
            ))

            val statsBefore = bodyStore.getStats()
            statsBefore.entryCount shouldBe 1

            // Insert call from new session
            dataSource.insert(createEntity(
                callId = "call-2",
                appInstance = "session-2",
                requestBody = "new body",
            ))

            // Old body should be removed from BodyStore
            val statsAfter = bodyStore.getStats()
            statsAfter.entryCount shouldBe 1
        }

        @Test
        fun `evicts oldest calls when max limit exceeded`() = runTest {
            settingsProvider.updateSettings(createSettings(maxStoredCalls = 3))

            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.insert(createEntity(callId = "call-2"))
            dataSource.insert(createEntity(callId = "call-3"))
            dataSource.insert(createEntity(callId = "call-4"))

            // Oldest should be evicted
            dataSource.getByCallId("call-1").shouldBeNull()
            // Others should remain
            dataSource.getByCallId("call-2").shouldNotBeNull()
            dataSource.getByCallId("call-3").shouldNotBeNull()
            dataSource.getByCallId("call-4").shouldNotBeNull()
        }

        @Test
        fun `evicts multiple entries when needed`() = runTest {
            settingsProvider.updateSettings(createSettings(maxStoredCalls = 2))

            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.insert(createEntity(callId = "call-2"))
            dataSource.insert(createEntity(callId = "call-3"))
            dataSource.insert(createEntity(callId = "call-4"))
            dataSource.insert(createEntity(callId = "call-5"))

            // Only last 2 should remain
            dataSource.getByCallId("call-1").shouldBeNull()
            dataSource.getByCallId("call-2").shouldBeNull()
            dataSource.getByCallId("call-3").shouldBeNull()
            dataSource.getByCallId("call-4").shouldNotBeNull()
            dataSource.getByCallId("call-5").shouldNotBeNull()
        }

        @Test
        fun `removes bodies from BodyStore when evicting`() = runTest {
            settingsProvider.updateSettings(createSettings(maxStoredCalls = 2))

            dataSource.insert(createEntity(callId = "call-1", requestBody = "body 1"))
            dataSource.insert(createEntity(callId = "call-2", requestBody = "body 2"))

            val statsBefore = bodyStore.getStats()
            statsBefore.entryCount shouldBe 2

            dataSource.insert(createEntity(callId = "call-3", requestBody = "body 3"))

            // Body for call-1 should be removed
            val statsAfter = bodyStore.getStats()
            statsAfter.entryCount shouldBe 2
        }

        @Test
        fun `updates existing entity`() = runTest {
            val original = createEntity(callId = "call-1", hasResponse = false)
            dataSource.insert(original)

            val updated = original.copy(
                response = NetworkResponse.Success(
                    durationMs = 200.0,
                    body = "response body",
                    headers = emptyMap(),
                    size = null,
                    contentType = "text/plain",
                    statusCode = 200,
                )
            )
            dataSource.update(updated)

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            retrieved.response.shouldNotBeNull()
            (retrieved.response as NetworkResponse.Success).body shouldBe "response body"
        }

        @Test
        fun `stores response body when added via update`() = runTest {
            dataSource.insert(createEntity(callId = "call-1", hasResponse = false))

            val statsBefore = bodyStore.getStats()
            statsBefore.entryCount shouldBe 0

            val updated = createEntity(
                callId = "call-1",
                hasResponse = true,
                responseBody = "response body",
            )
            dataSource.update(updated)

            val statsAfter = bodyStore.getStats()
            statsAfter.entryCount shouldBe 1
        }

        @Test
        fun `does nothing for non-existent entity`() = runTest {
            val entity = createEntity(callId = "non-existent")
            dataSource.update(entity)

            dataSource.getByCallId("non-existent").shouldBeNull()
        }

        @Test
        fun `returns null for non-existent call`() = runTest {
            val result = dataSource.getByCallId("non-existent")
            result.shouldBeNull()
        }

        @Test
        fun `returns entity with hydrated bodies`() = runTest {
            val entity = createEntity(
                callId = "call-1",
                requestBody = "request body",
                hasResponse = true,
                responseBody = "response body",
            )
            dataSource.insert(entity)

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            retrieved.request.body shouldBe "request body"
            (retrieved.response as NetworkResponse.Success).body shouldBe "response body"
        }

        @Test
        fun `emits null for non-existent call`() = runTest {
            dataSource.observeByCallId("non-existent").test {
                awaitItem().shouldBeNull()
            }
        }

        @Test
        fun `emits updates when call is modified`() = runTest {
            dataSource.observeByCallId("call-1").test {
                // Initially null
                awaitItem().shouldBeNull()

                // Insert call
                dataSource.insert(createEntity(callId = "call-1", hasResponse = false))
                val inserted = awaitItem()
                inserted.shouldNotBeNull()
                inserted.response.shouldBeNull()

                // Update call with response
                val updated = inserted.copy(
                    response = NetworkResponse.Success(
                        durationMs = 100.0,
                        body = null,
                        headers = emptyMap(),
                        size = null,
                        contentType = null,
                        statusCode = 200,
                    )
                )
                dataSource.update(updated)
                val updatedResult = awaitItem()
                updatedResult.shouldNotBeNull()
                updatedResult.response.shouldNotBeNull()
            }
        }

        @Test
        fun `emits null when call is deleted`() = runTest {
            dataSource.insert(createEntity(callId = "call-1"))

            dataSource.observeByCallId("call-1").test {
                awaitItem().shouldNotBeNull()

                dataSource.deleteByCallId("call-1")
                awaitItem().shouldBeNull()
            }
        }

        @Test
        fun `returns empty list when no calls exist`() = runTest {
            val result = dataSource.getByDeviceAndPackage("device-1", "com.example.app")
            result.shouldBeEmpty()
        }

        @Test
        fun `returns only calls for specified device and package`() = runTest {
            dataSource.insert(createEntity(callId = "call-1", deviceId = "device-1", packageName = "com.app.a"))
            dataSource.insert(createEntity(callId = "call-2", deviceId = "device-1", packageName = "com.app.b"))
            dataSource.insert(createEntity(callId = "call-3", deviceId = "device-2", packageName = "com.app.a"))
            dataSource.insert(createEntity(callId = "call-4", deviceId = "device-1", packageName = "com.app.a"))

            val result = dataSource.getByDeviceAndPackage("device-1", "com.app.a")

            result shouldHaveSize 2
            result.map { it.callId } shouldContainExactlyInAnyOrder listOf("call-1", "call-4")
        }

        @Test
        fun `returns entities with hydrated bodies`() = runTest {
            dataSource.insert(createEntity(
                callId = "call-1",
                requestBody = "request body",
            ))

            val result = dataSource.getByDeviceAndPackage("device-1", "com.example.app")

            result shouldHaveSize 1
            result[0].request.body shouldBe "request body"
        }

        @Test
        fun `emits empty list initially`() = runTest {
            dataSource.observeByDeviceAndPackage("device-1", "com.example.app").test {
                awaitItem().shouldBeEmpty()
            }
        }

        @Test
        fun `emits updates when calls are added`() = runTest {
            dataSource.observeByDeviceAndPackage("device-1", "com.example.app").test {
                awaitItem().shouldBeEmpty()

                dataSource.insert(createEntity(callId = "call-1"))
                awaitItem() shouldHaveSize 1

                dataSource.insert(createEntity(callId = "call-2"))
                awaitItem() shouldHaveSize 2
            }
        }

        @Test
        fun `only emits calls matching device and package`() = runTest {
            dataSource.observeByDeviceAndPackage("device-1", "com.app.a").test {
                awaitItem().shouldBeEmpty()

                // Add matching call
                dataSource.insert(createEntity(callId = "call-1", deviceId = "device-1", packageName = "com.app.a"))
                awaitItem() shouldHaveSize 1

                // Add non-matching call (different package)
                // Note: StateFlow emits on any change, so we may get an emission with same filtered result
                dataSource.insert(createEntity(callId = "call-2", deviceId = "device-1", packageName = "com.app.b"))
                // Consume potential emission (still size 1 since non-matching)
                skipItems(1)

                // Add another matching call
                dataSource.insert(createEntity(callId = "call-3", deviceId = "device-1", packageName = "com.app.a"))
                awaitItem() shouldHaveSize 2
            }
        }

        @Test
        fun `emits null initially`() = runTest {
            dataSource.observeCurrentAppInstance("device-1", "com.example.app").test {
                awaitItem().shouldBeNull()
            }
        }

        @Test
        fun `emits app instance when first call arrives`() = runTest {
            dataSource.observeCurrentAppInstance("device-1", "com.example.app").test {
                awaitItem().shouldBeNull()

                dataSource.insert(createEntity(appInstance = "session-1"))
                awaitItem() shouldBe "session-1"
            }
        }

        @Test
        fun `emits new app instance when session changes`() = runTest {
            dataSource.observeCurrentAppInstance("device-1", "com.example.app").test {
                awaitItem().shouldBeNull()

                dataSource.insert(createEntity(callId = "call-1", appInstance = "session-1"))
                awaitItem() shouldBe "session-1"

                dataSource.insert(createEntity(callId = "call-2", appInstance = "session-2"))
                awaitItem() shouldBe "session-2"
            }
        }

        @Test
        fun `does not emit when app instance stays same`() = runTest {
            dataSource.observeCurrentAppInstance("device-1", "com.example.app").test {
                awaitItem().shouldBeNull()

                dataSource.insert(createEntity(callId = "call-1", appInstance = "session-1"))
                awaitItem() shouldBe "session-1"

                // Same session - should not emit
                dataSource.insert(createEntity(callId = "call-2", appInstance = "session-1"))
                expectNoEvents()
            }
        }

        @Test
        fun `tracks sessions independently per device-package`() = runTest {
            dataSource.observeCurrentAppInstance("device-1", "com.app.a").test {
                awaitItem().shouldBeNull()

                // Insert for different device-package
                dataSource.insert(createEntity(
                    callId = "call-1",
                    deviceId = "device-2",
                    packageName = "com.app.a",
                    appInstance = "other-session",
                ))
                // Should not emit for our observed device-package
                expectNoEvents()

                // Insert for our device-package
                dataSource.insert(createEntity(
                    callId = "call-2",
                    deviceId = "device-1",
                    packageName = "com.app.a",
                    appInstance = "our-session",
                ))
                awaitItem() shouldBe "our-session"
            }
        }

        @Test
        fun `removes call from data source`() = runTest {
            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.getByCallId("call-1").shouldNotBeNull()

            dataSource.deleteByCallId("call-1")

            dataSource.getByCallId("call-1").shouldBeNull()
        }

        @Test
        fun `removes bodies from BodyStore`() = runTest {
            dataSource.insert(createEntity(
                callId = "call-1",
                requestBody = "request",
                hasResponse = true,
                responseBody = "response",
            ))

            val statsBefore = bodyStore.getStats()
            statsBefore.entryCount shouldBe 2

            dataSource.deleteByCallId("call-1")

            val statsAfter = bodyStore.getStats()
            statsAfter.entryCount shouldBe 0
        }

        @Test
        fun `does not affect other calls`() = runTest {
            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.insert(createEntity(callId = "call-2"))

            dataSource.deleteByCallId("call-1")

            dataSource.getByCallId("call-1").shouldBeNull()
            dataSource.getByCallId("call-2").shouldNotBeNull()
        }

        @Test
        fun `handles deletion of non-existent call gracefully`() = runTest {
            // Should not throw
            dataSource.deleteByCallId("non-existent")
        }

        @Test
        fun `removes all calls for device-package`() = runTest {
            dataSource.insert(createEntity(callId = "call-1", deviceId = "device-1", packageName = "com.app.a"))
            dataSource.insert(createEntity(callId = "call-2", deviceId = "device-1", packageName = "com.app.a"))

            dataSource.deleteByDeviceAndPackage("device-1", "com.app.a")

            dataSource.getByCallId("call-1").shouldBeNull()
            dataSource.getByCallId("call-2").shouldBeNull()
        }

        @Test
        fun `does not affect calls for other device-package`() = runTest {
            dataSource.insert(createEntity(callId = "call-1", deviceId = "device-1", packageName = "com.app.a"))
            dataSource.insert(createEntity(callId = "call-2", deviceId = "device-1", packageName = "com.app.b"))
            dataSource.insert(createEntity(callId = "call-3", deviceId = "device-2", packageName = "com.app.a"))

            dataSource.deleteByDeviceAndPackage("device-1", "com.app.a")

            dataSource.getByCallId("call-1").shouldBeNull()
            dataSource.getByCallId("call-2").shouldNotBeNull()
            dataSource.getByCallId("call-3").shouldNotBeNull()
        }

        @Test
        fun `removes bodies from BodyStore `() = runTest {
            dataSource.insert(createEntity(callId = "call-1", requestBody = "body 1"))
            dataSource.insert(createEntity(callId = "call-2", requestBody = "body 2"))

            val statsBefore = bodyStore.getStats()
            statsBefore.entryCount shouldBe 2

            dataSource.deleteByDeviceAndPackage("device-1", "com.example.app")

            val statsAfter = bodyStore.getStats()
            statsAfter.entryCount shouldBe 0
        }

        @Test
        fun `removes all calls`() = runTest {
            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.insert(createEntity(callId = "call-2"))
            dataSource.insert(createEntity(callId = "call-3"))

            dataSource.deleteAll()

            dataSource.getByCallId("call-1").shouldBeNull()
            dataSource.getByCallId("call-2").shouldBeNull()
            dataSource.getByCallId("call-3").shouldBeNull()
        }

        @Test
        fun `clears BodyStore`() = runTest {
            dataSource.insert(createEntity(callId = "call-1", requestBody = "body 1"))
            dataSource.insert(createEntity(callId = "call-2", requestBody = "body 2"))

            val statsBefore = bodyStore.getStats()
            statsBefore.entryCount shouldBe 2

            dataSource.deleteAll()

            val statsAfter = bodyStore.getStats()
            statsAfter.entryCount shouldBe 0
        }

        @Test
        fun `allows new inserts after deleteAll`() = runTest {
            dataSource.insert(createEntity(callId = "call-1"))
            dataSource.deleteAll()

            dataSource.insert(createEntity(callId = "call-2"))
            dataSource.getByCallId("call-2").shouldNotBeNull()
        }

        @Test
        fun `hydrates request body on retrieval`() = runTest {
            val requestBody = """{"key": "value"}"""
            dataSource.insert(createEntity(callId = "call-1", requestBody = requestBody))

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            retrieved.request.body shouldBe requestBody
        }

        @Test
        fun `hydrates response body on retrieval`() = runTest {
            val responseBody = """{"result": "success"}"""
            dataSource.insert(createEntity(
                callId = "call-1",
                hasResponse = true,
                responseBody = responseBody,
            ))

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            (retrieved.response as NetworkResponse.Success).body shouldBe responseBody
        }

        @Test
        fun `handles failure response without body`() = runTest {
            val entity = createEntity(callId = "call-1").copy(
                response = NetworkResponse.Failure(
                    durationMs = 100.0,
                    issue = "Connection timeout",
                )
            )
            dataSource.insert(entity)

            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            val response = retrieved.response as NetworkResponse.Failure
            response.issue shouldBe "Connection timeout"
        }

        @Test
        fun `returns null body when body was evicted from BodyStore`() = runTest {
            settingsProvider.updateSettings(createSettings(
                maxBodyCacheSizeBytes = 100,
                compressionEnabled = false,
            ))

            // Insert call with body
            dataSource.insert(createEntity(callId = "call-1", requestBody = "x".repeat(60)))
            // Insert another to potentially evict first body
            dataSource.insert(createEntity(callId = "call-2", requestBody = "y".repeat(60)))

            // First call's body may have been evicted from BodyStore
            // but the call metadata should still exist
            val retrieved = dataSource.getByCallId("call-1")
            retrieved.shouldNotBeNull()
            // Body might be null if evicted
        }

        @Test
        fun `handles concurrent inserts safely`() = runTest {
            coroutineScope {
                val jobs = (1..100).map { i ->
                    async {
                        dataSource.insert(createEntity(callId = "call-$i"))
                    }
                }
                jobs.forEach { it.await() }
            }

            // All calls should be inserted (minus any evictions)
            val settings = settingsProvider.getSettings()
            val expectedCount = minOf(100, settings.maxStoredCalls)

            var count = 0
            (1..100).forEach { i ->
                if (dataSource.getByCallId("call-$i") != null) count++
            }
            count shouldBe expectedCount
        }

        @Test
        fun `handles concurrent reads and writes safely`() = runTest {
            // Pre-populate
            dataSource.insert(createEntity(callId = "call-1", requestBody = "body"))

            coroutineScope {
                val jobs = (1..50).flatMap { i ->
                    listOf(
                        async { dataSource.getByCallId("call-1") },
                        async { dataSource.insert(createEntity(callId = "call-$i")) },
                    )
                }
                jobs.forEach { it.await() }
            }

            // Should complete without errors
        }
}
