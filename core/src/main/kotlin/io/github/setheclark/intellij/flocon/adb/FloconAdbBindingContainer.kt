package io.github.setheclark.intellij.flocon.adb

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import io.github.openflocon.data.core.adb.datasource.local.AdbLocalDataSource
import io.github.openflocon.domain.adb.ExecuteAdbCommandUseCase
import io.github.openflocon.domain.adb.repository.AdbRepository
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.openflocon.domain.settings.usecase.InitAdbPathUseCase
import io.github.openflocon.domain.settings.usecase.StartAdbForwardUseCase

@BindingContainer
interface FloconAdbBindingContainer {

    @Binds
    val InMemoryAdbLocalDataSource.bind: AdbLocalDataSource

    @Binds
    val AdbRepositoryImpl.bind: AdbRepository

    companion object {
        @Provides
        fun provideExecuteAdbCommandUseCase(
            adbRepository: AdbRepository,
            settingsRepository: SettingsRepository,
        ): ExecuteAdbCommandUseCase = ExecuteAdbCommandUseCase(adbRepository, settingsRepository)

        @Provides
        fun provideStartAdbForwardUseCase(
            executeAdbCommandUseCase: ExecuteAdbCommandUseCase,
        ): StartAdbForwardUseCase = StartAdbForwardUseCase(executeAdbCommandUseCase)

        @Provides
        fun provideInitAdbPathUseCase(
            settingsRepository: SettingsRepository,
            adbRepository: AdbRepository,
        ): InitAdbPathUseCase = InitAdbPathUseCase(settingsRepository, adbRepository)
    }
}
