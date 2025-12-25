package io.github.setheclark.intellij.managers.server

import co.touchlab.kermit.Logger
import com.flocon.data.remote.models.FloconIncomingMessageDataModel
import com.flocon.data.remote.network.models.FloconNetworkRequestDataModel
import com.flocon.data.remote.network.models.FloconNetworkResponseDataModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.Protocol
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.managers.network.NetworkEventProcessor
import kotlinx.serialization.json.Json

/**
 * Routes incoming messages from connected devices to the appropriate handlers.
 */
@SingleIn(AppScope::class)
@Inject
class MessageRouter(
    private val networkEventProcessor: NetworkEventProcessor,
    private val deviceRepository: DeviceRepository,
    private val json: Json,
) {
    private val log = Logger.withTag("MessageRouter")

    /**
     * Route an incoming message to the appropriate handler.
     */
    fun routeMessage(message: FloconIncomingMessageDataModel) {
        log.i { "Routing message: plugin=${message.plugin}, method=${message.method}" }

        // Update device info with name from message
        updateDeviceInfo(message)

        when (message.plugin) {
            Protocol.FromDevice.Network.Plugin -> handleNetworkMessage(message)
            Protocol.FromDevice.Device.Plugin -> handleDeviceMessage(message)
            else -> {
                log.d { "Ignoring message from plugin: ${message.plugin}" }
            }
        }
    }

    private fun updateDeviceInfo(message: FloconIncomingMessageDataModel) {
        deviceRepository.updateDevice(message.deviceId, message.appPackageName) { device ->
            device.copy(
                deviceName = message.deviceName,
                appName = message.appName,
            )
        }
    }

    private fun handleNetworkMessage(message: FloconIncomingMessageDataModel) {
        log.i { "Handling network message: method=${message.method}" }

        when (message.method) {
            Protocol.FromDevice.Network.Method.LogNetworkCallRequest -> {
                log.i { "Processing network request, body length=${message.body.length}" }
                try {
                    val request = json.decodeFromString<FloconNetworkRequestDataModel>(message.body)
                    val callId = request.floconCallId
                    log.i { "Parsed request: callId=$callId, url=${request.url}, method=${request.method}" }
                    if (callId == null) {
                        log.w { "Request has no floconCallId, skipping" }
                        return
                    }
                    networkEventProcessor.processRequest(
                        deviceId = message.deviceId,
                        packageName = message.appPackageName,
                        appInstance = message.appInstance,
                        callId = callId,
                        request = request,
                    )
                } catch (e: Exception) {
                    log.e(e) { "Failed to parse network request: ${e.message}" }
                    log.e { "Body was: ${message.body.take(500)}" }
                }
            }

            Protocol.FromDevice.Network.Method.LogNetworkCallResponse -> {
                log.i { "Processing network response, body length=${message.body.length}" }
                try {
                    val response = json.decodeFromString<FloconNetworkResponseDataModel>(message.body)
                    val callId = response.floconCallId
                    log.i { "Parsed response: callId=$callId, status=${response.responseHttpCode}" }
                    if (callId == null) {
                        log.w { "Response has no floconCallId, skipping" }
                        return
                    }
                    networkEventProcessor.processResponse(
                        deviceId = message.deviceId,
                        packageName = message.appPackageName,
                        appInstance = message.appInstance,
                        callId = callId,
                        response = response,
                    )
                } catch (e: Exception) {
                    log.e(e) { "Failed to parse network response: ${e.message}" }
                    log.e { "Body was: ${message.body.take(500)}" }
                }
            }

            Protocol.FromDevice.Network.Method.LogWebSocketEvent -> {
                log.i { "WebSocket event received" }
            }

            else -> {
                log.w { "Unknown network method: ${message.method}" }
            }
        }
    }

    private fun handleDeviceMessage(message: FloconIncomingMessageDataModel) {
        when (message.method) {
            Protocol.FromDevice.Device.Method.RegisterDevice -> {
                log.i { "Device registered: ${message.deviceName} (${message.appPackageName})" }
            }

            else -> {
                log.d { "Unknown device method: ${message.method}" }
            }
        }
    }
}
