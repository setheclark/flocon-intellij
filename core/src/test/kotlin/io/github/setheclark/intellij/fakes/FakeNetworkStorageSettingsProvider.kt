package io.github.setheclark.intellij.fakes

import io.github.setheclark.intellij.settings.NetworkStorageSettings
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of [NetworkStorageSettingsProvider] for testing.
 * Allows controlling settings without IntelliJ platform dependencies.
 */
class FakeNetworkStorageSettingsProvider(
    initialSettings: NetworkStorageSettings = NetworkStorageSettings(),
) : NetworkStorageSettingsProvider {

    private val _settings = MutableStateFlow(initialSettings)

    override val settings: StateFlow<NetworkStorageSettings> = _settings.asStateFlow()

    override fun getSettings(): NetworkStorageSettings = _settings.value

    override fun updateSettings(settings: NetworkStorageSettings) {
        _settings.value = settings
    }
}
