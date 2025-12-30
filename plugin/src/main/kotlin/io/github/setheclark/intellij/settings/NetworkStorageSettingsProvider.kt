package io.github.setheclark.intellij.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Provider for network storage settings.
 * Provides reactive access to settings configuration.
 */
interface NetworkStorageSettingsProvider {

    /**
     * Current settings as a reactive flow.
     */
    val settings: StateFlow<NetworkStorageSettings>

    /**
     * Get current settings snapshot.
     */
    fun getSettings(): NetworkStorageSettings

    /**
     * Update settings.
     */
    fun updateSettings(settings: NetworkStorageSettings)
}

/**
 * Update settings using a transform function.
 */
inline fun NetworkStorageSettingsProvider.updateSettings(
    transform: (NetworkStorageSettings) -> NetworkStorageSettings
) {
    updateSettings(transform(getSettings()))
}
