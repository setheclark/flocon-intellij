package io.github.setheclark.intellij.managers.network

import co.touchlab.kermit.Logger
import com.flocon.data.remote.network.models.FloconNetworkRequestDataModel
import com.flocon.data.remote.network.models.FloconNetworkResponseDataModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkRequest
import io.github.setheclark.intellij.domain.models.NetworkResponse

/**
 * Processes network events from connected devices and updates the repository.
 * Transforms raw data models into domain models.
 */
@SingleIn(AppScope::class)
@Inject
class NetworkEventProcessor(
    private val networkCallRepository: NetworkCallRepository,
) {
    private val log = Logger.withTag("NetworkEventProcessor")

    /**
     * Process an incoming network request.
     * Creates a new NetworkCallEntry and adds it to the repository.
     */
    fun processRequest(
        deviceId: String,
        packageName: String,
        appInstance: Long,
        callId: String,
        request: FloconNetworkRequestDataModel,
    ) {
        log.i { "Processing request: ${request.method} ${request.url}" }

        val requestHeaders = request.requestHeaders ?: emptyMap()
        val entry = NetworkCallEntry(
            id = callId,
            deviceId = deviceId,
            packageName = packageName,
            request = NetworkRequest(
                url = request.url ?: "",
                method = request.method ?: "UNKNOWN",
                headers = requestHeaders,
                body = request.requestBody,
                contentType = requestHeaders.entries.find {
                    it.key.equals("Content-Type", ignoreCase = true)
                }?.value,
                size = request.requestSize,
            ),
            startTime = request.startTime ?: System.currentTimeMillis(),
        )

        networkCallRepository.addCall(entry)
        log.i { "Added network call: ${entry.request.method} ${entry.request.url}" }
    }

    /**
     * Process an incoming network response.
     * Updates the existing NetworkCallEntry with the response data.
     */
    fun processResponse(
        deviceId: String,
        packageName: String,
        appInstance: Long,
        callId: String,
        response: FloconNetworkResponseDataModel,
    ) {
        log.i { "Processing response for callId=$callId: ${response.responseHttpCode} (${response.durationMs}ms)" }

        networkCallRepository.updateCall(callId) { call ->
            call.copy(
                response = NetworkResponse(
                    statusCode = response.responseHttpCode ?: 0,
                    statusMessage = null,
                    headers = response.responseHeaders ?: emptyMap(),
                    body = response.responseBody,
                    contentType = response.responseContentType,
                    size = response.responseSize,
                    error = response.responseError,
                ),
                duration = response.durationMs?.toLong(),
            )
        }
    }
}
