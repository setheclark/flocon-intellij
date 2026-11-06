package io.github.setheclark.intellij.network

import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.setheclark.intellij.flocon.network.NetworkMessageMapper
import io.github.setheclark.intellij.flocon.network.NetworkRequest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class NetworkMessageMapperGraphQlTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = NetworkMessageMapper(json)

    private fun requestType(requestBody: String): NetworkRequest.Type {
        val body = JsonObject(
            mapOf(
                "floconCallId" to JsonPrimitive("call-1"),
                "url" to JsonPrimitive("https://example.com/graphql"),
                "method" to JsonPrimitive("POST"),
                "startTime" to JsonPrimitive(1L),
                "requestHeaders" to JsonObject(emptyMap()),
                "requestBody" to JsonPrimitive(requestBody),
            ),
        )
        val message = FloconIncomingMessageDomainModel(
            deviceName = "device",
            deviceId = "device-id",
            plugin = "network",
            body = json.encodeToString(JsonObject.serializer(), body),
            method = "logNetworkCallRequest",
            appName = "app",
            appPackageName = "com.example",
            appInstance = 1L,
            platform = "android",
            floconVersionOnDevice = "1.0",
        )
        return mapper.toCallEntity(message)!!.request.type
    }

    @Test
    fun `persisted query has operation name and unknown operation type`() {
        val type = requestType(
            """{"operationName":"GetUser","extensions":{"persistedQuery":{"version":1,"sha256Hash":"abc"}}}""",
        )

        type.shouldBeInstanceOf<NetworkRequest.Type.GraphQl>()
        type.operationName shouldBe "GetUser"
        type.persisted shouldBe true
        type.query shouldBe null
        type.operationType shouldBe null
    }

    @Test
    fun `full query text reports its real operation type`() {
        val type = requestType("""{"operationName":"AddUser","query":"mutation AddUser { addUser { id } }"}""")

        type.shouldBeInstanceOf<NetworkRequest.Type.GraphQl>()
        type.operationType shouldBe "mutation"
        type.persisted shouldBe false
    }

    @Test
    fun `anonymous query reports its real operation type with no name`() {
        val type = requestType("""{"query":"query { me { id } }"}""")

        type.shouldBeInstanceOf<NetworkRequest.Type.GraphQl>()
        type.operationName shouldBe null
        type.operationType shouldBe "query"
    }
}
