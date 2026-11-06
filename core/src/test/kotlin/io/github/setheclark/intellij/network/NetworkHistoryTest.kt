package io.github.setheclark.intellij.network

import com.flocon.data.remote.models.FloconIncomingMessageDataModel
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.fakes.FakeServer
import io.github.setheclark.intellij.flocon.messages.MessageRemoteDataSourceImpl
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkMessageMapper
import io.github.setheclark.intellij.flocon.network.NetworkRepositoryImpl
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.server.MessageServerStatusDataSource
import io.github.setheclark.intellij.ui.network.usecase.ObserveFilteredNetworkRequestsUseCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.sql.DriverManager

/**
 * Network history through the fake server, on a real SQLite file with the real driver.
 * A "restart" is a new [Harness] on the same database file.
 */
class NetworkHistoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val harnesses = mutableListOf<Harness>()

    private val databaseFile: File get() = File(tmp.root, "history/network-history.db")

    @After
    fun tearDown() {
        harnesses.forEach { it.close() }
    }

    private fun harness(file: File = databaseFile) = Harness(file).also { harnesses += it }

    private fun unopenableFile() = File(tmp.newFile("blocker"), "network-history.db")

    @Test
    fun `calls and bodies are still present after a restart`() = runBlocking<Unit> {
        val first = harness()
        first.sendCall("call-1", appInstance = 1, requestBody = """{"q":1}""", responseBody = """{"ok":true}""")
        first.awaitSession { it.singleOrNull()?.response != null }
        first.close()

        val second = harness()
        second.status.mode.value shouldBe NetworkHistoryMode.Persistent

        val calls = second.awaitSession { it.isNotEmpty() }
        calls.single().callId shouldBe "call-1"
        val stored = second.dataSource.getByCallId("call-1")!!
        stored.request.body shouldBe """{"q":1}"""
        stored.request.headers shouldBe mapOf("Accept" to "application/json")
        stored.response.shouldBeInstanceOf<NetworkResponse.Success>().apply {
            body shouldBe """{"ok":true}"""
            statusCode shouldBe 200
            contentType shouldBe "application/json"
        }
    }

    @Test
    fun `a new app session keeps the previous session and lists only the new one`() = runBlocking<Unit> {
        val first = harness()
        first.sendCall("old-1", appInstance = 1)
        first.sendCall("old-2", appInstance = 1)
        first.awaitSession { it.size == 2 }
        first.close()

        val second = harness()
        second.sendCall("new-1", appInstance = 2)

        second.awaitSession { it.map(NetworkCallEntity::callId) == listOf("new-1") }
        second.dataSource.observeCurrentAppInstance(DEVICE, PACKAGE).first { it == "2" }
        second.dataSource.getByCallId("old-1")!!.appInstance shouldBe "1"
        second.dataSource.getByCallId("old-2")!!.appInstance shouldBe "1"
    }

    @Test
    fun `sessions of other devices and apps do not leak into the list`() = runBlocking<Unit> {
        val h = harness()
        h.sendCall("mine", appInstance = 1)
        h.sendCall("other-device", appInstance = 1, deviceId = "device-2")
        h.sendCall("other-app", appInstance = 1, packageName = "com.other")

        h.awaitSession { it.map(NetworkCallEntity::callId) == listOf("mine") }
    }

    @Test
    fun `a body larger than one megabyte is stored in full`() = runBlocking<Unit> {
        val body = buildString {
            repeat(300_000) { append("line-").append(it).append('\n') }
        }
        (body.length > 3 * 1024 * 1024) shouldBe true

        val h = harness()
        h.sendCall("big", appInstance = 1, responseBody = body)
        h.awaitSession { it.singleOrNull()?.response != null }

        val stored = h.dataSource.getByCallId("big")!!
        stored.response.shouldBeInstanceOf<NetworkResponse.Success>().body shouldBe body
    }

    @Test
    fun `live updates arrive as calls and responses come in`() = runBlocking<Unit> {
        val h = harness()

        h.sendCall("call-1", appInstance = 1, withResponse = false)
        val pending = h.awaitSession { it.size == 1 }
        pending.single().response shouldBe null

        h.sendResponse("call-1", responseBody = "done")
        val answered = h.awaitSession { it.singleOrNull()?.response != null }
        answered.single().response.shouldBeInstanceOf<NetworkResponse.Success>()

        h.sendCall("call-2", appInstance = 1, withResponse = false)
        h.awaitSession { it.size == 2 }.map { it.callId } shouldBe listOf("call-1", "call-2")
    }

    @Test
    fun `lists do not carry bodies but details do`() = runBlocking<Unit> {
        val h = harness()
        h.sendCall("call-1", appInstance = 1, requestBody = "req", responseBody = "res")
        val listed = h.awaitSession { it.singleOrNull()?.response != null }.single()

        listed.request.body shouldBe null
        listed.response.shouldBeInstanceOf<NetworkResponse.Success>().body shouldBe null
        h.repository.observeCall("call-1").first { it?.request?.body == "req" }
    }

    @Test
    fun `searching finds calls by request or response body in the current session only`() = runBlocking<Unit> {
        val h = harness()
        h.sendCall("old", appInstance = 1, requestBody = "Needle in the old session", responseBody = "x")
        h.sendCall("req", appInstance = 2, requestBody = """{"q":"Needle"}""", responseBody = "x")
        h.sendCall("res", appInstance = 2, requestBody = "x", responseBody = "a NEEDLE here")
        h.sendCall("none", appInstance = 2, requestBody = "x", responseBody = "x")
        h.sendCall("other-app", appInstance = 2, packageName = "com.other", requestBody = "needle")
        h.awaitSession { it.map(NetworkCallEntity::callId).containsAll(listOf("req", "res", "none")) && it.all { c -> c.response != null } }

        h.search("needle") shouldBe listOf("req", "res")
        h.search("absent") shouldBe emptyList()
    }

    @Test
    fun `memory-only history is searchable by body too`() = runBlocking<Unit> {
        val h = harness(unopenableFile())
        h.sendCall("req", appInstance = 1, requestBody = "Needle", responseBody = "x")
        h.sendCall("none", appInstance = 1, requestBody = "x", responseBody = "x")
        h.awaitSession { it.size == 2 && it.all { c -> c.response != null } }

        h.search("needle") shouldBe listOf("req")
    }

    @Test
    fun `deleting a call deletes its bodies`() = runBlocking<Unit> {
        val h = harness()
        h.sendCall("call-1", appInstance = 1, requestBody = "req", responseBody = "res")
        h.sendCall("call-2", appInstance = 1, requestBody = "req", responseBody = "res")
        h.awaitSession { it.size == 2 && it.all { call -> call.response != null } }
        bodyCount() shouldBe 4

        h.dataSource.deleteByCallId("call-1")
        bodyCount() shouldBe 2
        h.dataSource.getByCallId("call-1") shouldBe null

        h.dataSource.deleteAll()
        bodyCount() shouldBe 0
    }

    @Test
    fun `a corrupt database is recreated`() = runBlocking<Unit> {
        databaseFile.parentFile.mkdirs()
        databaseFile.writeText("this is not a sqlite database ".repeat(500))

        val h = harness()
        h.status.mode.value shouldBe NetworkHistoryMode.Persistent

        h.sendCall("call-1", appInstance = 1)
        h.awaitSession { it.size == 1 }
    }

    @Test
    fun `a schema version mismatch recreates the database`() = runBlocking<Unit> {
        val first = harness()
        first.sendCall("call-1", appInstance = 1)
        first.awaitSession { it.size == 1 }
        first.close()
        setUserVersion(99)

        val second = harness()
        second.status.mode.value shouldBe NetworkHistoryMode.Persistent
        second.dataSource.getByCallId("call-1") shouldBe null

        second.sendCall("call-2", appInstance = 1)
        second.awaitSession { it.map(NetworkCallEntity::callId) == listOf("call-2") }
    }

    @Test
    fun `a database that cannot be opened falls back to memory-only history`() = runBlocking<Unit> {
        // The database directory's parent is a regular file, so neither opening nor recreating can succeed.
        val h = harness(unopenableFile())

        h.status.mode.value shouldBe NetworkHistoryMode.MemoryOnly

        h.sendCall("call-1", appInstance = 1, requestBody = "req", responseBody = "res")
        h.awaitSession { it.singleOrNull()?.response != null }
        h.dataSource.getByCallId("call-1")!!.request.body shouldBe "req"
    }

    @Test
    fun `memory-only history keeps its fixed limits`() = runBlocking<Unit> {
        val h = harness(unopenableFile())

        repeat(1_005) { h.sendCall("call-$it", appInstance = 1, withResponse = false) }
        val calls = h.awaitSession { it.lastOrNull()?.callId == "call-1004" }

        calls shouldHaveSize 1_000
        calls.first().callId shouldBe "call-5"

        val tooBig = "x".repeat(2 * 1024 * 1024)
        h.sendCall("big", appInstance = 1, responseBody = tooBig)
        h.awaitSession { it.lastOrNull()?.callId == "big" && it.last().response != null }
        val stored = h.dataSource.getByCallId("big")!!.response.shouldBeInstanceOf<NetworkResponse.Success>()
        (stored.body!!.length < tooBig.length) shouldBe true
    }

    @Test
    fun `memory-only history keeps the previous session and lists only the new one`() = runBlocking<Unit> {
        val h = harness(unopenableFile())
        h.sendCall("old", appInstance = 1)
        h.sendCall("new", appInstance = 2)

        h.awaitSession { it.map(NetworkCallEntity::callId) == listOf("new") }
        h.dataSource.getByCallId("old")!!.appInstance shouldBe "1"
    }

    @Test
    fun `an empty database lists nothing`() = runBlocking<Unit> {
        harness().awaitSession { true }.shouldBeEmpty()
    }

    private fun bodyCount(): Int = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT count(*) FROM network_body").use {
                it.next()
                it.getInt(1)
            }
        }
    }

    private fun setUserVersion(version: Int) {
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { c ->
            c.createStatement().use { it.execute("PRAGMA user_version = $version") }
        }
    }

    private class Harness(file: File) {
        private val json = Json { ignoreUnknownKeys = true }
        private val scope = CoroutineScope(Dispatchers.Default)
        private val server = FakeServer()
        private val dispatchers = object : DispatcherProvider {
            override val ui: CoroutineDispatcher = Dispatchers.Default
            override val viewModel: CoroutineDispatcher = Dispatchers.Default
            override val domain: CoroutineDispatcher = Dispatchers.Default
            override val data: CoroutineDispatcher = Dispatchers.IO
        }

        val status = NetworkHistoryStatusDataSource()
        val dataSource: NetworkDataSource = NetworkDataSourceFactory(
            location = object : NetworkHistoryLocation {
                override val databaseFile: File = file
            },
            status = status,
            dispatchers = dispatchers,
            json = json,
        ).create()
        val repository = NetworkRepositoryImpl(
            dispatchers = dispatchers,
            networkDataSource = dataSource,
            mapper = NetworkMessageMapper(json),
        )

        private val receiver: Job = scope.launch(Dispatchers.Unconfined) {
            MessageRemoteDataSourceImpl(server, MessageServerStatusDataSource())
                .listenMessages()
                .collect { message -> repository.onMessageReceived(message.address(), message.normalized()) }
        }

        fun close() {
            receiver.cancel()
            scope.cancel()
        }

        suspend fun sendCall(
            callId: String,
            appInstance: Long,
            deviceId: String = DEVICE,
            packageName: String = PACKAGE,
            requestBody: String? = null,
            responseBody: String? = null,
            withResponse: Boolean = true,
        ) {
            send(
                method = "logNetworkCallRequest",
                deviceId = deviceId,
                packageName = packageName,
                appInstance = appInstance,
                body = JsonObject(
                    buildMap {
                        put("floconCallId", JsonPrimitive(callId))
                        put("url", JsonPrimitive("https://example.com/api/$callId"))
                        put("method", JsonPrimitive("GET"))
                        put("startTime", JsonPrimitive(1L))
                        put("requestHeaders", JsonObject(mapOf("Accept" to JsonPrimitive("application/json"))))
                        requestBody?.let { put("requestBody", JsonPrimitive(it)) }
                    },
                ),
            )
            if (withResponse) {
                sendResponse(callId, responseBody, deviceId, packageName, appInstance)
            }
        }

        suspend fun sendResponse(
            callId: String,
            responseBody: String?,
            deviceId: String = DEVICE,
            packageName: String = PACKAGE,
            appInstance: Long = 1,
        ) {
            send(
                method = "logNetworkCallResponse",
                deviceId = deviceId,
                packageName = packageName,
                appInstance = appInstance,
                body = JsonObject(
                    buildMap {
                        put("floconCallId", JsonPrimitive(callId))
                        put("durationMs", JsonPrimitive(12.5))
                        put("responseHttpCode", JsonPrimitive(200))
                        put("responseContentType", JsonPrimitive("application/json"))
                        put("responseHeaders", JsonObject(mapOf("Content-Type" to JsonPrimitive("application/json"))))
                        responseBody?.let { put("responseBody", JsonPrimitive(it)) }
                    },
                ),
            )
        }

        /** Calls of the current session that match [text] in the call list filter, as the UI sees them. */
        suspend fun search(text: String): List<String> =
            ObserveFilteredNetworkRequestsUseCase(repository)(
                DeviceIdAndPackageNameDomainModel(DEVICE, PACKAGE, 0),
                NetworkFilter(searchText = text),
            ).first().map { it.callId }

        /** Waits until the current session of the default device and app satisfies [predicate]. */
        suspend fun awaitSession(predicate: (List<NetworkCallEntity>) -> Boolean): List<NetworkCallEntity> =
            withTimeout(30_000) { repository.observeCalls(DEVICE, PACKAGE).first(predicate) }

        private suspend fun send(method: String, deviceId: String, packageName: String, appInstance: Long, body: JsonObject) {
            server.simulateMessage(
                FloconIncomingMessageDataModel(
                    deviceName = "Pixel",
                    deviceId = deviceId,
                    plugin = "network",
                    body = json.encodeToString(JsonObject.serializer(), body),
                    method = method,
                    appName = "App",
                    appPackageName = packageName,
                    appInstance = appInstance,
                    platform = "android",
                    versionName = "1.0",
                ),
            )
        }

        private fun FloconIncomingMessageDomainModel.address() = DeviceIdAndPackageNameDomainModel(
            deviceId = deviceId,
            packageName = appPackageName,
            appInstance = appInstance,
        )

        // The real use case matches plugin and method names in lowercase.
        private fun FloconIncomingMessageDomainModel.normalized() = copy(plugin = plugin.lowercase(), method = method.lowercase())
    }

    private companion object {
        const val DEVICE = "device-1"
        const val PACKAGE = "com.example.app"
    }
}
