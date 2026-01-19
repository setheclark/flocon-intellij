package io.github.setheclark.intellij.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Provider for network storage settings.
 * Provides reactive access to settings configuration.
 */
interface PluginSettingsProvider {

    /**
     * Current settings as a reactive flow.
     */
    val settings: StateFlow<PluginSettings>

    /**
     * Get current settings snapshot.
     */
    fun getSettings(): PluginSettings

    /**
     * Update settings.
     */
    fun updateSettings(settings: PluginSettings)
}

/**
 * Update settings using a transform function.
 */
inline fun PluginSettingsProvider.updateSettings(
    transform: (PluginSettings) -> PluginSettings
) {
    updateSettings(transform(getSettings()))
}
