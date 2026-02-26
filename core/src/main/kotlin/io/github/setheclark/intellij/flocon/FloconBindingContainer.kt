package io.github.setheclark.intellij.flocon

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.device.usecase.HandleNewAppUseCase
import io.github.openflocon.domain.messages.repository.MessagesRepository
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.setheclark.intellij.flocon.adb.FloconAdbBindingContainer
import io.github.setheclark.intellij.flocon.coroutines.DispatcherProviderImpl
import io.github.setheclark.intellij.flocon.device.FloconDeviceBindingContainer
import io.github.setheclark.intellij.flocon.messages.FloconMessagesBindingContainer
import io.github.setheclark.intellij.flocon.network.FloconNetworkBindingContainer
import io.github.setheclark.intellij.flocon.settings.SettingsRepositoryImpl

@BindingContainer(
    includes = [
        FloconAdbBindingContainer::class,
        FloconDeviceBindingContainer::class,
        FloconMessagesBindingContainer::class,
        FloconNetworkBindingContainer::class,
    ],
)
interface FloconBindingContainer {

    @Binds
    val DispatcherProviderImpl.bind: DispatcherProvider

    @Binds
    val SettingsRepositoryImpl.bind: SettingsRepository

    companion object {

        @Provides
        fun provideHandleNewAppUseCase(
            devicesRepository: DevicesRepository,
        ): HandleNewAppUseCase = HandleNewAppUseCase(devicesRepository)

        @Provides
        fun provideStartServerUseCase(
            messagesRepository: MessagesRepository,
        ): StartServerUseCase = StartServerUseCase(messagesRepository)
    }
}
