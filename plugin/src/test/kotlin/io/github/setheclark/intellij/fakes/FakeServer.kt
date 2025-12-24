package io.github.setheclark.intellij.fakes

import com.flocon.data.remote.models.FloconDeviceIdAndPackageNameDataModel
import com.flocon.data.remote.models.FloconIncomingMessageDataModel
import com.flocon.data.remote.models.FloconOutgoingMessageDataModel
import com.flocon.data.remote.server.Server
import io.github.openflocon.domain.messages.models.FloconReceivedFileDomainModel
import io.github.setheclark.intellij.services.ServerFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/**
 * Fake implementation of Server for testing.
 * Allows simulating incoming messages and device connections.
 */
class FakeServer : Server {

    private val _receivedMessages = MutableSharedFlow<FloconIncomingMessageDataModel>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val receivedMessages: Flow<FloconIncomingMessageDataModel> = _receivedMessages

    private val _activeDevices = MutableStateFlow<Set<FloconDeviceIdAndPackageNameDataModel>>(emptySet())
    override val activeDevices: Flow<Set<FloconDeviceIdAndPackageNameDataModel>> = _activeDevices

    private val _receivedFiles = MutableSharedFlow<FloconReceivedFileDomainModel>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val receivedFiles: Flow<FloconReceivedFileDomainModel> = _receivedFiles

    var isWebSocketStarted = false
        private set
    var isHttpStarted = false
        private set
    var isStopped = false
        private set
    var webSocketPort: Int? = null
        private set
    var httpPort: Int? = null
        private set

    val sentMessages = mutableListOf<Pair<FloconDeviceIdAndPackageNameDataModel, FloconOutgoingMessageDataModel>>()

    override fun startWebsocket(port: Int) {
        isWebSocketStarted = true
        webSocketPort = port
        isStopped = false
    }

    override fun starHttp(port: Int) {
        isHttpStarted = true
        httpPort = port
    }

    override fun stop() {
        isStopped = true
        isWebSocketStarted = false
        isHttpStarted = false
    }

    override suspend fun sendMessageToClient(
        deviceIdAndPackageName: FloconDeviceIdAndPackageNameDataModel,
        message: FloconOutgoingMessageDataModel
    ) {
        sentMessages.add(deviceIdAndPackageName to message)
    }

    /**
     * Simulate receiving a message from a device.
     */
    suspend fun simulateMessage(message: FloconIncomingMessageDataModel) {
        _receivedMessages.emit(message)
    }

    /**
     * Simulate a device connecting.
     */
    fun simulateDeviceConnected(device: FloconDeviceIdAndPackageNameDataModel) {
        _activeDevices.value = _activeDevices.value + device
    }

    /**
     * Simulate a device disconnecting.
     */
    fun simulateDeviceDisconnected(device: FloconDeviceIdAndPackageNameDataModel) {
        _activeDevices.value = _activeDevices.value - device
    }

    /**
     * Reset all state.
     */
    fun reset() {
        isWebSocketStarted = false
        isHttpStarted = false
        isStopped = false
        webSocketPort = null
        httpPort = null
        sentMessages.clear()
        _activeDevices.value = emptySet()
    }
}

/**
 * Fake implementation of ServerFactory that returns a FakeServer.
 */
class FakeServerFactory(
    val server: FakeServer = FakeServer()
) : ServerFactory {
    override fun createServer(json: Json): Server = server
}
