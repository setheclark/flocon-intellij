package io.github.setheclark.intellij.fixtures

import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkRequest
import io.github.setheclark.intellij.domain.models.NetworkResponse
import java.util.UUID

/**
 * Factory for creating test instances of network call related objects.
 */
object NetworkCallFactory {

    fun createRequest(
        url: String = "https://api.example.com/test",
        method: String = "GET",
        headers: Map<String, String> = mapOf("Content-Type" to "application/json"),
        body: String? = null,
        contentType: String? = headers["Content-Type"],
        size: Long? = body?.length?.toLong()
    ): NetworkRequest = NetworkRequest(
        url = url,
        method = method,
        headers = headers,
        body = body,
        contentType = contentType,
        size = size
    )

    fun createResponse(
        statusCode: Int = 200,
        statusMessage: String? = "OK",
        headers: Map<String, String> = mapOf("Content-Type" to "application/json"),
        body: String? = """{"success": true}""",
        contentType: String? = headers["Content-Type"],
        size: Long? = body?.length?.toLong(),
        error: String? = null
    ): NetworkResponse = NetworkResponse(
        statusCode = statusCode,
        statusMessage = statusMessage,
        headers = headers,
        body = body,
        contentType = contentType,
        size = size,
        error = error
    )

    fun createEntry(
        id: String = UUID.randomUUID().toString(),
        deviceId: String = "test-device-001",
        packageName: String = "com.example.testapp",
        request: NetworkRequest = createRequest(),
        response: NetworkResponse? = null,
        startTime: Long = System.currentTimeMillis(),
        duration: Long? = null
    ): NetworkCallEntry = NetworkCallEntry(
        id = id,
        deviceId = deviceId,
        packageName = packageName,
        request = request,
        response = response,
        startTime = startTime,
        duration = duration
    )

    /**
     * Creates a complete network call entry with both request and response.
     */
    fun createCompletedEntry(
        id: String = UUID.randomUUID().toString(),
        deviceId: String = "test-device-001",
        packageName: String = "com.example.testapp",
        url: String = "https://api.example.com/test",
        method: String = "GET",
        statusCode: Int = 200,
        requestBody: String? = null,
        responseBody: String? = """{"success": true}""",
        duration: Long = 150L
    ): NetworkCallEntry = NetworkCallEntry(
        id = id,
        deviceId = deviceId,
        packageName = packageName,
        request = createRequest(url = url, method = method, body = requestBody),
        response = createResponse(statusCode = statusCode, body = responseBody),
        startTime = System.currentTimeMillis() - duration,
        duration = duration
    )

    /**
     * Creates a list of network call entries for testing.
     */
    fun createEntries(count: Int, deviceId: String = "test-device-001"): List<NetworkCallEntry> {
        return (1..count).map { index ->
            createCompletedEntry(
                id = "call-$index",
                deviceId = deviceId,
                url = "https://api.example.com/endpoint$index",
                method = if (index % 2 == 0) "POST" else "GET",
                statusCode = if (index % 5 == 0) 404 else 200,
                duration = (50L * index)
            )
        }
    }
}
