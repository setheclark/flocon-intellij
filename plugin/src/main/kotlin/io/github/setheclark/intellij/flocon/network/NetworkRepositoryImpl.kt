package io.github.setheclark.intellij.flocon.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.Protocol
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.setheclark.intellij.flocon.network.datasource.NetworkDataSource
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
class NetworkRepositoryImpl(
    private val dispatchers: DispatcherProvider,
    private val networkDataSource: NetworkDataSource,
    private val mapper: NetworkMessageMapper,
) : NetworkRepository, MessagesReceiverRepository {

    val log = Logger.withPluginTag("NetworkRepository")

    override fun observeCalls(deviceId: String, packageName: String): Flow<List<NetworkCallEntity>> =
        networkDataSource.observeByDeviceAndPackage(deviceId, packageName)

    override fun observeCall(callId: String): Flow<NetworkCallEntity?> =
        networkDataSource.observeByCallId(callId)

    // region MessagesReceiverRepository

    override val pluginName: List<String> = listOf(
        Protocol.FromDevice.Network.Plugin,
    )

    override suspend fun onMessageReceived(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        message: FloconIncomingMessageDomainModel
    ) {
        withContext(dispatchers.data) {
            when (message.method) {
                Protocol.FromDevice.Network.Method.LogNetworkCallRequest -> {
                    log.v { "LogNetworkCallRequest" }
                    val request = mapper.toCallEntity(message)

                    if (request == null) {
                        log.e { "request instance is null for $message" }
                    } else {
                        networkDataSource.insert(request)
                    }
                }

                Protocol.FromDevice.Network.Method.LogNetworkCallResponse -> {
                    log.v { "LogNetworkCallResponse" }
                    val response = mapper.toResponseModel(message) ?: run {
                        log.e { "callId is null for $message" }
                        return@withContext
                    }
                    val request = response.floconCallId?.let { networkDataSource.getByCallId(it) } ?: run {
                        log.e { "message for $response.floconCallId not found" }
                        return@withContext
                    }

                    val entity = mapper.updateCallEntity(request, response)
                    networkDataSource.update(entity)
                }

                else -> {
                    log.v { "Ignored message: $message" }
                }
            }
        }
    }

    override suspend fun onDeviceConnected(
        deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel,
        isNewDevice: Boolean
    ) = Unit

    // endregion
}