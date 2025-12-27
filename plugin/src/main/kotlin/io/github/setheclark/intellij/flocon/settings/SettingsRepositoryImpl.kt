package io.github.setheclark.intellij.flocon.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.models.settings.NetworkSettings
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.setheclark.intellij.adb.AdbStatusManager
import io.github.setheclark.intellij.domain.models.AdbStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Inject
@SingleIn(AppScope::class)
class SettingsRepositoryImpl(
    private val adbStatusManager: AdbStatusManager,
) : SettingsRepository {

    private val _networkSettings = MutableStateFlow(
        NetworkSettings(
            pinnedDetails = false,
            displayOldSessions = false,
            autoScroll = true,
            invertList = true,
        )
    )

    override var networkSettings: NetworkSettings
        get() = _networkSettings.value
        set(value) {
            _networkSettings.value = value
        }

    override val networkSettingsFlow: Flow<NetworkSettings> = _networkSettings

    override fun getAdbPath(): String? {
        return (adbStatusManager.status.value as? AdbStatus.Available)?.path
    }

    override suspend fun setAdbPath(path: String) {
        adbStatusManager.setAdbPath(path)
    }

    override suspend fun setFontSizeMultiplier(value: Float) {
        //noop
    }

    override val adbPath: Flow<String?>
        get() = TODO("Not yet implemented")
    override val fontSizeMultiplier: StateFlow<Float>
        get() = TODO("Not yet implemented")
}