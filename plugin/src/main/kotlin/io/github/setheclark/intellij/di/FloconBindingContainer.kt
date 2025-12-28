package io.github.setheclark.intellij.di

import com.flocon.data.remote.device.datasource.DeviceRemoteDataSourceImpl
import com.flocon.data.remote.network.datasource.NetworkRemoteDataSourceImpl
import com.flocon.data.remote.server.Server
import com.flocon.data.remote.server.ServerJvm
import dev.zacsweers.metro.*
import io.github.openflocon.data.core.adb.datasource.local.AdbLocalDataSource
import io.github.openflocon.data.core.device.datasource.local.LocalCurrentDeviceDataSource
import io.github.openflocon.data.core.device.datasource.local.LocalDevicesDataSource
import io.github.openflocon.data.core.device.datasource.remote.RemoteDeviceDataSource
import io.github.openflocon.data.core.messages.datasource.MessageRemoteDataSource
import io.github.openflocon.data.core.network.datasource.*
import io.github.openflocon.data.core.network.repository.NetworkRepositoryImpl
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.openflocon.domain.messages.repository.MessagesRepository
import io.github.openflocon.domain.network.repository.NetworkBadQualityRepository
import io.github.openflocon.domain.network.repository.NetworkImageRepository
import io.github.openflocon.domain.network.repository.NetworkMocksRepository
import io.github.openflocon.domain.network.repository.NetworkRepository
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.setheclark.intellij.flocon.adb.AdbLocalDataSourceImpl
import io.github.setheclark.intellij.flocon.adb.AdbRepositoryImpl
import io.github.setheclark.intellij.flocon.coroutines.DispatcherProviderImpl
import io.github.setheclark.intellij.flocon.device.DevicesRepositoryImpl
import io.github.setheclark.intellij.flocon.device.LocalCurrentDeviceDataSourceImpl
import io.github.setheclark.intellij.flocon.device.LocalDevicesDataSourceImpl
import io.github.setheclark.intellij.flocon.messages.MessageRemoteDataSourceImpl
import io.github.setheclark.intellij.flocon.messages.MessagesRepositoryImpl
import io.github.setheclark.intellij.flocon.network.*
import io.github.setheclark.intellij.flocon.settings.SettingsRepositoryImpl
import io.github.setheclark.intellij.network.PluginNetworkRepositoryImpl
import kotlinx.serialization.json.Json

@BindingContainer
interface FloconBindingContainer {

    @Binds
    val AdbLocalDataSourceImpl.bind: AdbLocalDataSource

    @Binds
    val AdbRepositoryImpl.bind: AdbRepository

    @Binds
    val DispatcherProviderImpl.bind: DispatcherProvider

    @Binds
    val SettingsRepositoryImpl.bind: SettingsRepository

    @Binds
    val DevicesRepositoryImpl.bind: DevicesRepository

    @Binds
    val NetworkImageRepositoryImpl.bind: NetworkImageRepository

    @Binds
    val NetworkQualityLocalDataSourceImpl.bind: NetworkQualityLocalDataSource

    @Binds
    val LocalDevicesDataSourceImpl.bind: LocalDevicesDataSource

    @Binds
    val NetworkLocalDataSourceImpl.bind: NetworkLocalDataSource

    @Binds
    val MessagesRepositoryImpl.bind: MessagesRepository

    @Binds
    val MessageRemoteDataSourceImpl.bind: MessageRemoteDataSource

    @Binds
    val NetworkReplayDataSourceImpl.bind: NetworkReplayDataSource

    @Binds
    val NetworkLocalWebsocketDataSourceImpl.bind: NetworkLocalWebsocketDataSource

    @Binds
    val NetworkMocksLocalDataSourceImpl.bind: NetworkMocksLocalDataSource

    @Binds
    val LocalCurrentDeviceDataSourceImpl.bind: LocalCurrentDeviceDataSource

    companion object {

        @Provides
        @SingleIn(AppScope::class)
        fun provideServer(
            json: Json
        ): Server = ServerJvm(json)

        @Provides
        @SingleIn(AppScope::class)
        fun provideNetworkRemoteDataSource(
            json: Json,
            server: Server
        ): NetworkRemoteDataSource = NetworkRemoteDataSourceImpl(server, json)

//        @Provides
//        @SingleIn(AppScope::class)
//        fun providesNetworkRepositoryImpl(
//            dispatcherProvider: DispatcherProvider,
//            networkLocalDataSource: NetworkLocalDataSource,
//            networkLocalWebsocketDataSource: NetworkLocalWebsocketDataSource,
//            networkMocksLocalDataSource: NetworkMocksLocalDataSource,
//            networkQualityLocalDataSource: NetworkQualityLocalDataSource,
//            networkImageRepository: NetworkImageRepository,
//            networkRemoteDataSource: NetworkRemoteDataSource,
//            networkReplayDataSource: NetworkReplayDataSource,
//        ): NetworkRepositoryImpl {
//            return NetworkRepositoryImpl(
//                dispatcherProvider = dispatcherProvider,
//                networkLocalDataSource = networkLocalDataSource,
//                networkLocalWebsocketDataSource = networkLocalWebsocketDataSource,
//                networkMocksLocalDataSource = networkMocksLocalDataSource,
//                networkQualityLocalDataSource = networkQualityLocalDataSource,
//                networkImageRepository = networkImageRepository,
//                networkRemoteDataSource = networkRemoteDataSource,
//                networkReplayDataSource = networkReplayDataSource,
//            )
//        }

        @Provides
        fun providesNetworkRepository(
            impl: NetworkRepositoryImpl,
        ): NetworkRepository = impl

        @Provides
        fun providesNetworkMocksRepository(
            impl: NetworkRepositoryImpl,
        ): NetworkMocksRepository = impl

        @Provides
        fun providesNetworkBadQualityRepository(
            impl: NetworkRepositoryImpl,
        ): NetworkBadQualityRepository = impl

        @Provides
        @SingleIn(AppScope::class)
        fun provideRemoteDeviceDataSource(
            json: Json,
            server: Server,
        ): RemoteDeviceDataSource = DeviceRemoteDataSourceImpl(json, server)
    }
}