package io.github.setheclark.intellij.fakes

import io.github.openflocon.domain.models.settings.AppScaleSetting
import io.github.openflocon.domain.models.settings.NetworkSettings
import io.github.openflocon.domain.models.settings.ServerPortsDomainModel
import io.github.openflocon.domain.models.settings.ThemeSetting
import io.github.openflocon.domain.settings.repository.AdbForwardStatus
import io.github.openflocon.domain.settings.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [SettingsRepository]; tests mostly care about [getAdbPath]. */
class FakeSettingsRepository(private var path: String? = "/sdk/adb") : SettingsRepository {
    override var networkSettings = NetworkSettings(
        pinnedDetails = false,
        displayOldSessions = false,
        autoScroll = false,
        invertList = false,
    )
    override val networkSettingsFlow: Flow<NetworkSettings> = MutableStateFlow(networkSettings)

    override fun getAdbPath(): String? = path
    override suspend fun setAdbPath(path: String) {
        this.path = path
    }

    override suspend fun setAppScale(value: AppScaleSetting) = Unit
    override suspend fun setTheme(value: ThemeSetting) = Unit
    override fun getFloconDocumentsPath(): String? = null
    override suspend fun setFloconDocumentsPath(path: String) = Unit
    override val floconDocumentsPath: Flow<String?> = MutableStateFlow(null)
    override fun getDismissedDesktopVersion(): String? = null
    override suspend fun setDismissedDesktopVersion(version: String) = Unit
    override val dismissedClientVersionFlow: Flow<String?> = MutableStateFlow(null)
    override fun getDismissedClientVersion(): String? = null
    override suspend fun setDismissedClientVersion(version: String) = Unit
    override fun getLastSeenReleaseNotesVersion(): String? = null
    override suspend fun setLastSeenReleaseNotesVersion(version: String) = Unit
    override val savedServerPorts: Flow<ServerPortsDomainModel> = MutableStateFlow(ServerPortsDomainModel())
    override suspend fun setServerPorts(ports: ServerPortsDomainModel) = Unit
    override val adbPath: Flow<String?> = MutableStateFlow(path)
    override val appScale: StateFlow<AppScaleSetting> = MutableStateFlow(AppScaleSetting.DEFAULT)
    override val theme: StateFlow<ThemeSetting> = MutableStateFlow(ThemeSetting.System)
    override val adbForwardStatus: StateFlow<AdbForwardStatus> = MutableStateFlow(AdbForwardStatus.UNKNOWN)
    override fun setAdbForwardStatus(status: AdbForwardStatus) = Unit
}
