package io.github.setheclark.intellij.flocon.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.models.settings.NetworkSettings
import io.github.openflocon.domain.models.settings.ThemeSetting
import io.github.openflocon.domain.settings.repository.AdbForwardStatus
import io.github.openflocon.domain.settings.repository.SettingsRepository
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.adb.AdbStatusDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
class SettingsRepositoryImpl(
    private val adbStatusDataSource: AdbStatusDataSource,
) : SettingsRepository {

    private val _networkSettings = MutableStateFlow(
        NetworkSettings(
            pinnedDetails = false,
            displayOldSessions = false,
            autoScroll = true,
            invertList = true,
        ),
    )

    override var networkSettings: NetworkSettings
        get() = _networkSettings.value
        set(value) {
            _networkSettings.value = value
        }

    override val networkSettingsFlow: Flow<NetworkSettings> = _networkSettings

    override fun getAdbPath(): String? {
        return (adbStatusDataSource.status.value as? AdbStatus.Available)?.path
    }

    override suspend fun setAdbPath(path: String) {
        adbStatusDataSource.setAdbPath(path)
    }

    override val adbPath: Flow<String?> = adbStatusDataSource.status.map { status ->
        when (status) {
            is AdbStatus.Available -> status.path
            AdbStatus.Initializing, AdbStatus.NotFound -> null
        }
    }

    // TODO: persist these via NetworkStorageSettingsState; in-memory only until the related features are adopted
    private val _floconDocumentsPath = MutableStateFlow<String?>(null)
    private val _dismissedDesktopVersion = MutableStateFlow<String?>(null)
    private val _dismissedClientVersion = MutableStateFlow<String?>(null)

    override fun getFloconDocumentsPath(): String? = _floconDocumentsPath.value

    override suspend fun setFloconDocumentsPath(path: String) {
        _floconDocumentsPath.value = path
    }

    override val floconDocumentsPath: Flow<String?> = _floconDocumentsPath

    override fun getDismissedDesktopVersion(): String? = _dismissedDesktopVersion.value

    override suspend fun setDismissedDesktopVersion(version: String) {
        _dismissedDesktopVersion.value = version
    }

    override val dismissedClientVersionFlow: Flow<String?> = _dismissedClientVersion

    override fun getDismissedClientVersion(): String? = _dismissedClientVersion.value

    override suspend fun setDismissedClientVersion(version: String) {
        _dismissedClientVersion.value = version
    }

    override suspend fun setFontSizeMultiplier(value: Float) {
        // noop
    }

    override val fontSizeMultiplier: StateFlow<Float>
        get() = TODO("no-op")

    private val _theme = MutableStateFlow(ThemeSetting.System)

    override suspend fun setTheme(value: ThemeSetting) {
        _theme.value = value
    }

    override val theme: StateFlow<ThemeSetting> = _theme

    private val _adbForwardStatus = MutableStateFlow(AdbForwardStatus.UNKNOWN)

    override fun setAdbForwardStatus(status: AdbForwardStatus) {
        _adbForwardStatus.value = status
    }

    override val adbForwardStatus: StateFlow<AdbForwardStatus> = _adbForwardStatus
}
