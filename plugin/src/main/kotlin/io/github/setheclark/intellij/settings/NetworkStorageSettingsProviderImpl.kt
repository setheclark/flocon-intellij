package io.github.setheclark.intellij.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * IntelliJ platform implementation of [NetworkStorageSettingsProvider].
 * Wraps the IntelliJ PersistentStateComponent and provides reactive access to settings.
 */
@Inject
@SingleIn(AppScope::class)
class NetworkStorageSettingsProviderImpl : NetworkStorageSettingsProvider {

    private val state = NetworkStorageSettingsState.getInstance()
    private val _settings = MutableStateFlow(state.toSettings())

    override val settings: StateFlow<NetworkStorageSettings> = _settings.asStateFlow()

    override fun getSettings(): NetworkStorageSettings = _settings.value

    override fun updateSettings(settings: NetworkStorageSettings) {
        state.updateFrom(settings)
        _settings.value = settings
    }
}
