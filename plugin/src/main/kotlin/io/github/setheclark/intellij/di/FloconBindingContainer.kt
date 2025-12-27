package io.github.setheclark.intellij.di

import com.flocon.data.remote.network.datasource.NetworkRemoteDataSourceImpl
import com.flocon.data.remote.server.Server
import com.flocon.data.remote.server.ServerJvm
import dev.zacsweers.metro.*
import io.github.openflocon.data.core.adb.datasource.local.AdbLocalDataSource
import io.github.openflocon.data.core.network.datasource.*
import io.github.openflocon.data.core.network.repository.NetworkRepositoryImpl
import io.github.openflocon.domain.adb.ExecuteAdbCommandUseCase
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.device.usecase.HandleDeviceAndAppUseCase
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.openflocon.domain.network.repository.NetworkBadQualityRepository
import io.github.openflocon.domain.network.repository.NetworkImageRepository
import io.github.openflocon.domain.network.repository.NetworkMocksRepository
import io.github.openflocon.domain.network.repository.NetworkRepository
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.openflocon.domain.settings.usecase.StartAdbForwardUseCase
import io.github.setheclark.intellij.flocon.adb.AdbLocalDataSourceImpl
import io.github.setheclark.intellij.flocon.adb.AdbRepositoryImpl
import io.github.setheclark.intellij.flocon.coroutines.DispatcherProviderImpl
import io.github.setheclark.intellij.flocon.device.DevicesRepositoryImpl
import io.github.setheclark.intellij.flocon.network.NetworkImageRepositoryImpl
import io.github.setheclark.intellij.flocon.network.NetworkQualityLocalDataSourceImpl
import io.github.setheclark.intellij.flocon.settings.SettingsRepositoryImpl
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

    companion object {

        @Provides
        fun provideExecuteAdbCommandUseCase(
            adbRepository: AdbRepository,
            settingsRepository: SettingsRepository,
        ): ExecuteAdbCommandUseCase = ExecuteAdbCommandUseCase(adbRepository, settingsRepository)

        @Provides
        fun provideStartAdbForwardUseCase(
            executeAdbCommandUseCase: ExecuteAdbCommandUseCase
        ): StartAdbForwardUseCase = StartAdbForwardUseCase(executeAdbCommandUseCase)

        @Provides
        fun provideDeviceAndAppUseCase(
            devicesRepository: DevicesRepository,
        ): HandleDeviceAndAppUseCase = HandleDeviceAndAppUseCase(devicesRepository)

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

        @Provides
        @SingleIn(AppScope::class)
        fun providesNetworkRepositoryImpl(
            dispatcherProvider: DispatcherProvider,
            networkLocalDataSource: NetworkLocalDataSource,
            networkLocalWebsocketDataSource: NetworkLocalWebsocketDataSource,
            networkMocksLocalDataSource: NetworkMocksLocalDataSource,
            networkQualityLocalDataSource: NetworkQualityLocalDataSource,
            networkImageRepository: NetworkImageRepository,
            networkRemoteDataSource: NetworkRemoteDataSource,
            networkReplayDataSource: NetworkReplayDataSource,
        ): NetworkRepositoryImpl {
            return NetworkRepositoryImpl(
                dispatcherProvider = dispatcherProvider,
                networkLocalDataSource = networkLocalDataSource,
                networkLocalWebsocketDataSource = networkLocalWebsocketDataSource,
                networkMocksLocalDataSource = networkMocksLocalDataSource,
                networkQualityLocalDataSource = networkQualityLocalDataSource,
                networkImageRepository = networkImageRepository,
                networkRemoteDataSource = networkRemoteDataSource,
                networkReplayDataSource = networkReplayDataSource,

                )
        }

        @Provides
        fun providesNetworkRepository(
            impl: NetworkRepositoryImpl,
        ): NetworkRepository = impl

        @Provides
        fun providesNetworkMocksRepository(
            impl: NetworkRepositoryImpl,
        ): NetworkMocksRepository = impl

        @Provides
        fun providesMessagesReceiverRepository(
            impl: NetworkRepositoryImpl,
        ): MessagesReceiverRepository = impl

        @Provides
        fun providesNetworkBadQualityRepository(
            impl: NetworkRepositoryImpl,
        ): NetworkBadQualityRepository = impl
    }
}