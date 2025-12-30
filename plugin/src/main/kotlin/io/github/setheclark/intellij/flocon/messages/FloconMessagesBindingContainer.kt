package io.github.setheclark.intellij.flocon.messages

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.github.openflocon.data.core.messages.datasource.MessageRemoteDataSource
import io.github.openflocon.domain.device.usecase.HandleDeviceAndAppUseCase
import io.github.openflocon.domain.device.usecase.HandleNewAppUseCase
import io.github.openflocon.domain.device.usecase.HandleNewDeviceUseCase
import io.github.openflocon.domain.messages.repository.MessagesReceiverRepository
import io.github.openflocon.domain.messages.repository.MessagesRepository
import io.github.openflocon.domain.messages.usecase.HandleIncomingMessagesUseCase
import io.github.setheclark.intellij.flocon.device.DevicesRepositoryImpl
import io.github.setheclark.intellij.flocon.network.NetworkRepositoryImpl
import io.github.setheclark.intellij.server.PluginMessageRemoteDataSource

@BindingContainer
interface FloconMessagesBindingContainer {

    @Binds
    val MessageRemoteDataSourceImpl.binds: MessageRemoteDataSource

    @Binds
    val MessageRemoteDataSourceImpl.binds2: PluginMessageRemoteDataSource

    @Binds
    val MessagesRepositoryImpl.binds: MessagesRepository

    companion object {

        @Provides
        @IntoSet
        fun provideDevicesMessageReceiver(
            impl: DevicesRepositoryImpl
        ): MessagesReceiverRepository = impl

        @Provides
        @IntoSet
        fun provideNetworkMessageReceiver(
            impl: NetworkRepositoryImpl
        ): MessagesReceiverRepository = impl

        @Provides
        fun provideHandleIncomingMessagesUseCase(
            messagesRepository: MessagesRepository,
            plugins: Set<MessagesReceiverRepository>,
            handleDeviceAndAppUseCase: HandleDeviceAndAppUseCase,
            handleNewDeviceUseCase: HandleNewDeviceUseCase,
            handleNewAppUseCase: HandleNewAppUseCase,
        ): HandleIncomingMessagesUseCase {
            return HandleIncomingMessagesUseCase(
                messagesRepository,
                plugins = plugins.toList(),
                handleDeviceAndAppUseCase,
                handleNewDeviceUseCase,
                handleNewAppUseCase,
            )
        }
    }
}