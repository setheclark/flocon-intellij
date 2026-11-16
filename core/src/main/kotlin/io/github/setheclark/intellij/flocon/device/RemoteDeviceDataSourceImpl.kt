package io.github.setheclark.intellij.flocon.device

import com.flocon.data.remote.device.model.BonjourDataModel
import com.flocon.data.remote.device.model.RegisterDeviceDataModel
import com.flocon.data.remote.models.FloconOutgoingMessageDataModel
import com.flocon.data.remote.models.toRemote
import com.flocon.data.remote.server.Server
import dev.zacsweers.metro.Inject
import io.github.openflocon.data.core.device.datasource.remote.RemoteDeviceDataSource
import io.github.openflocon.domain.Protocol
import io.github.openflocon.domain.device.models.DeviceConnectionType
import io.github.openflocon.domain.device.models.DeviceIdAndPackageNameDomainModel
import io.github.openflocon.domain.messages.models.FloconIncomingMessageDomainModel
import io.github.setheclark.intellij.util.safeDecode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Inject
class RemoteDeviceDataSourceImpl(
    private val json: Json,
    private val server: Server,
) : RemoteDeviceDataSource {

    override val activeDevices: Flow<Set<DeviceIdAndPackageNameDomainModel>> = server.activeDevices
        .map { devices ->
            devices.map {
                DeviceIdAndPackageNameDomainModel(
                    deviceId = it.deviceId,
                    packageName = it.packageName,
                    appInstance = it.appInstance,
                )
            }.toSet()
        }.distinctUntilChanged()

    override fun getDeviceSerial(message: FloconIncomingMessageDomainModel): String? =
        json.safeDecode<RegisterDeviceDataModel>(message.body)?.serial

    override fun getInstalledPlugins(message: FloconIncomingMessageDomainModel): Set<String>? =
        json.safeDecode<BonjourDataModel>(message.body)
            ?.plugins
            ?.map { it.lowercase() }
            ?.toSet()

    override fun getConnectionType(message: FloconIncomingMessageDomainModel): DeviceConnectionType? =
        json.safeDecode<BonjourDataModel>(message.body)
            ?.connectionType
            ?.let { if (it == "wifi") DeviceConnectionType.Wifi else DeviceConnectionType.Default }

    override fun getSdk(message: FloconIncomingMessageDomainModel): String? =
        json.safeDecode<BonjourDataModel>(message.body)?.sdk

    override suspend fun askForDeviceAppIcon(deviceIdAndPackageName: DeviceIdAndPackageNameDomainModel) {
        server.sendMessageToClient(
            deviceIdAndPackageName = deviceIdAndPackageName.toRemote(),
            message = FloconOutgoingMessageDataModel(
                plugin = Protocol.ToDevice.Device.Plugin,
                method = Protocol.ToDevice.Device.Method.GetAppIcon,
                body = "",
            ),
        )
    }
}
