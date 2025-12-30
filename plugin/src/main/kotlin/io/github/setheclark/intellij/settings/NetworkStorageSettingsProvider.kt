package io.github.setheclark.intellij.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Injectable provider for network storage settings.
 * Wraps the IntelliJ PersistentStateComponent and provides reactive access to settings.
 */
@Inject
@SingleIn(AppScope::class)
class NetworkStorageSettingsProvider {

    private val state = NetworkStorageSettingsState.getInstance()
    private val _settings = MutableStateFlow(state.toSettings())

    /**
     * Current settings as a reactive flow.
     */
    val settings: StateFlow<NetworkStorageSettings> = _settings.asStateFlow()

    /**
     * Get current settings snapshot.
     */
    fun getSettings(): NetworkStorageSettings = _settings.value

    /**
     * Update settings. Changes are persisted automatically.
     */
    fun updateSettings(settings: NetworkStorageSettings) {
        state.updateFrom(settings)
        _settings.value = settings
    }

    /**
     * Update settings using a transform function.
     */
    inline fun updateSettings(transform: (NetworkStorageSettings) -> NetworkStorageSettings) {
        updateSettings(transform(getSettings()))
    }
}
