package io.github.setheclark.intellij.di

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.*
import io.github.openflocon.data.core.network.datasource.*
import io.github.openflocon.data.core.network.repository.NetworkRepositoryImpl
import io.github.openflocon.domain.adb.ExecuteAdbCommandUseCase
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.device.usecase.*
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.openflocon.domain.messages.repository.MessagesRepository
import io.github.openflocon.domain.messages.usecase.HandleIncomingMessagesUseCase
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.openflocon.domain.network.repository.NetworkImageRepository
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.openflocon.domain.settings.usecase.StartAdbForwardUseCase
import io.github.setheclark.intellij.flocon.device.DevicesRepositoryImpl
import io.github.setheclark.intellij.flocon.network.NetworkLocalDataSourceImpl
import io.github.setheclark.intellij.network.PluginNetworkRepository
import io.github.setheclark.intellij.network.PluginNetworkRepositoryImpl

@BindingContainer
class UseCaseBindingContainer {

    @Provides
    @SingleIn(AppScope::class)
    fun providePluginNetworkRepository(
        dispatcherProvider: DispatcherProvider,
        networkLocalDataSource: NetworkLocalDataSourceImpl,
        networkLocalWebsocketDataSource: NetworkLocalWebsocketDataSource,
        networkMocksLocalDataSource: NetworkMocksLocalDataSource,
        networkQualityLocalDataSource: NetworkQualityLocalDataSource,
        networkImageRepository: NetworkImageRepository,
        networkRemoteDataSource: NetworkRemoteDataSource,
        networkReplayDataSource: NetworkReplayDataSource,
    ): PluginNetworkRepository {
        val delegate = NetworkRepositoryImpl(
            dispatcherProvider = dispatcherProvider,
            networkLocalDataSource = networkLocalDataSource,
            networkLocalWebsocketDataSource = networkLocalWebsocketDataSource,
            networkMocksLocalDataSource = networkMocksLocalDataSource,
            networkQualityLocalDataSource = networkQualityLocalDataSource,
            networkImageRepository = networkImageRepository,
            networkRemoteDataSource = networkRemoteDataSource,
            networkReplayDataSource = networkReplayDataSource,
        )
        return PluginNetworkRepositoryImpl(
            delegate = delegate,
            networkLocalDataSource = networkLocalDataSource,
        )
    }

    @Provides
    @IntoSet
    fun providesPluginNetworkMessagesReceiverRepository(
        impl: PluginNetworkRepository,
    ): MessagesReceiverRepository = impl

    @Provides
    @IntoSet
    fun provideDevicesMessagesReceiverRepository(
        impl: DevicesRepositoryImpl,
    ): MessagesReceiverRepository = impl

    @Provides
    fun provideHandleIncomingMessagesUseCase(
        messagesRepository: MessagesRepository,
        plugins: Set<MessagesReceiverRepository>,
        handleDeviceAndAppUseCase: HandleDeviceAndAppUseCase,
        handleNewDeviceUseCase: HandleNewDeviceUseCase,
        handleNewAppUseCase: HandleNewAppUseCase,
    ): HandleIncomingMessagesUseCase {
        Logger.withTag("<<<Plugins>>>").w { plugins.joinToString { it.pluginName.toString() } }
        return HandleIncomingMessagesUseCase(
            messagesRepository,
            plugins = plugins.toList(),
            handleDeviceAndAppUseCase,
            handleNewDeviceUseCase,
            handleNewAppUseCase,
        )
    }

    @Provides
    fun provideStartServerUseCase(
        messagesRepository: MessagesRepository,
    ): StartServerUseCase = StartServerUseCase(messagesRepository)

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
    fun provideHandleDeviceAndAppUseCase(
        devicesRepository: DevicesRepository,
    ): HandleDeviceAndAppUseCase = HandleDeviceAndAppUseCase(devicesRepository)

    @Provides
    fun provideHandleNewDeviceUseCase(
        adbRepository: AdbRepository,
        settingsRepository: SettingsRepository,
    ): HandleNewDeviceUseCase {
        return HandleNewDeviceUseCase(adbRepository, settingsRepository)
    }

    @Provides
    fun provideHandleNewAppUseCase(
        devicesRepository: DevicesRepository,
    ): HandleNewAppUseCase {
        return HandleNewAppUseCase(devicesRepository)
    }

    @Provides
    fun provideObserveCurrentDeviceIdUseCase(
        deviceRepository: DevicesRepository
    ): ObserveCurrentDeviceIdUseCase {
        return ObserveCurrentDeviceIdUseCase(deviceRepository)
    }

    @Provides
    fun provideObserveCurrentDeviceIdAndPackageNameUseCase(
        deviceRepository: DevicesRepository,
        observeCurrentDeviceIdUseCase: ObserveCurrentDeviceIdUseCase
    ): ObserveCurrentDeviceIdAndPackageNameUseCase {
        return ObserveCurrentDeviceIdAndPackageNameUseCase(observeCurrentDeviceIdUseCase, deviceRepository)
    }
}