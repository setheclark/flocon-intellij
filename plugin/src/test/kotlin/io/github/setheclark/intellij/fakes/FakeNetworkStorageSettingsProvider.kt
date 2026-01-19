package io.github.setheclark.intellij.fakes

import io.github.setheclark.intellij.settings.PluginSettings
import io.github.setheclark.intellij.settings.PluginSettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [PluginSettingsProvider] for testing.
 * Allows controlling settings without IntelliJ platform dependencies.
 */
class FakeNetworkStorageSettingsProvider(
    initialSettings: PluginSettings = PluginSettings()
) : PluginSettingsProvider {

    private val _settings = MutableStateFlow(initialSettings)

    override val settings: StateFlow<PluginSettings> = _settings.asStateFlow()

    override fun getSettings(): PluginSettings = _settings.value

    override fun updateSettings(settings: PluginSettings) {
        _settings.value = settings
    }
}
