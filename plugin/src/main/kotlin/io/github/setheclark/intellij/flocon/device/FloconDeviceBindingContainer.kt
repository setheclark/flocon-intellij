package io.github.setheclark.intellij.flocon.device

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import io.github.openflocon.data.core.device.datasource.local.LocalCurrentDeviceDataSource
import io.github.openflocon.data.core.device.datasource.local.LocalDevicesDataSource
import io.github.openflocon.data.core.device.datasource.remote.RemoteDeviceDataSource
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.device.repository.DevicesRepository
import io.github.openflocon.domain.device.usecase.GetCurrentDeviceIdUseCase
import io.github.openflocon.domain.device.usecase.HandleDeviceAndAppUseCase
import io.github.openflocon.domain.device.usecase.HandleNewDeviceUseCase
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdAndPackageNameUseCase
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdUseCase
import io.github.openflocon.domain.device.usecase.ObserveDevicesUseCase
import io.github.openflocon.domain.device.usecase.SelectDeviceAppUseCase
import io.github.openflocon.domain.device.usecase.SelectDeviceUseCase
import io.github.openflocon.domain.settings.repository.SettingsRepository

@BindingContainer
interface FloconDeviceBindingContainer {

    @Binds
    val DevicesRepositoryImpl.binds: DevicesRepository

    @Binds
    val RemoteDeviceDataSourceImpl.binds: RemoteDeviceDataSource

    @Binds
    val InMemoryLocalCurrentDeviceDataSource.bind: LocalCurrentDeviceDataSource

    @Binds
    val InMemoryLocalDevicesDataSource.bind: LocalDevicesDataSource

    companion object {
        @Provides
        fun provideHandleDeviceAndAppUseCase(
            devicesRepository: DevicesRepository
        ): HandleDeviceAndAppUseCase = HandleDeviceAndAppUseCase(devicesRepository)

        @Provides
        fun provideHandleNewDeviceUseCase(
            adbRepository: AdbRepository,
            settingsRepository: SettingsRepository,
        ): HandleNewDeviceUseCase = HandleNewDeviceUseCase(adbRepository, settingsRepository)

        @Provides
        fun provideGetCurrentDeviceIdUseCase(
            devicesRepository: DevicesRepository,
        ): GetCurrentDeviceIdUseCase = GetCurrentDeviceIdUseCase(devicesRepository)

        @Provides
        fun provideObserveCurrentDeviceIdUseCase(
            devicesRepository: DevicesRepository,
        ): ObserveCurrentDeviceIdUseCase = ObserveCurrentDeviceIdUseCase(devicesRepository)

        @Provides
        fun provideObserveCurrentDeviceIdAndPackageNameUseCase(
            observeCurrentDeviceIdUseCase: ObserveCurrentDeviceIdUseCase,
            devicesRepository: DevicesRepository,
        ): ObserveCurrentDeviceIdAndPackageNameUseCase =
            ObserveCurrentDeviceIdAndPackageNameUseCase(observeCurrentDeviceIdUseCase, devicesRepository)

        @Provides
        fun provideSelectDeviceUseCase(
            devicesRepository: DevicesRepository,
        ): SelectDeviceUseCase = SelectDeviceUseCase(devicesRepository)

        @Provides
        fun provideSelectDeviceAppUseCase(
            devicesRepository: DevicesRepository,
            getCurrentDeviceIdUseCase: GetCurrentDeviceIdUseCase,
        ): SelectDeviceAppUseCase = SelectDeviceAppUseCase(devicesRepository, getCurrentDeviceIdUseCase)

        @Provides
        fun provideObserveDevicesUseCase(
            devicesRepository: DevicesRepository,
        ): ObserveDevicesUseCase = ObserveDevicesUseCase(devicesRepository)
    }
}