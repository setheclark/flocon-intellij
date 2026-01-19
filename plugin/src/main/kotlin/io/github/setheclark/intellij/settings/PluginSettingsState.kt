package io.github.setheclark.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent state component for network storage settings.
 * Settings are stored in the IDE's configuration directory.
 *
 * Also implements [PluginSettingsProvider] to provide reactive access to settings.
 */
@Service(Service.Level.APP)
@State(
    name = "FloconNetworkStorageSettings",
    storages = [Storage("flocon-network-storage.xml")]
)
class PluginSettingsState : PersistentStateComponent<PluginSettingsState>, PluginSettingsProvider {

    var maxStoredCalls: Int = PluginSettings.DEFAULT_MAX_STORED_CALLS
    var maxBodyCacheSizeBytes: Long = PluginSettings.DEFAULT_MAX_BODY_CACHE_SIZE_BYTES
    var maxBodySizeBytes: Int = PluginSettings.DEFAULT_MAX_BODY_SIZE_BYTES
    var compressionEnabled: Boolean = PluginSettings.DEFAULT_COMPRESSION_ENABLED
    var mcpServerEnabled: Boolean = PluginSettings.DEFAULT_MCP_SERVER_ENABLED
    var mcpServerPort: Int = PluginSettings.DEFAULT_MCP_SERVER_PORT

    // Reactive flow for settings changes
    private val _settings = MutableStateFlow(toSettings())
    override val settings: StateFlow<PluginSettings> = _settings.asStateFlow()

    override fun getState(): PluginSettingsState = this

    override fun loadState(state: PluginSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
        // Update flow when state is loaded from disk
        _settings.value = toSettings()
    }

    fun toSettings(): PluginSettings = PluginSettings(
        maxStoredCalls = maxStoredCalls,
        maxBodyCacheSizeBytes = maxBodyCacheSizeBytes,
        maxBodySizeBytes = maxBodySizeBytes,
        compressionEnabled = compressionEnabled,
        mcpServerEnabled = mcpServerEnabled,
        mcpServerPort = mcpServerPort,
    )

    fun updateFrom(settings: PluginSettings) {
        maxStoredCalls = settings.maxStoredCalls
        maxBodyCacheSizeBytes = settings.maxBodyCacheSizeBytes
        maxBodySizeBytes = settings.maxBodySizeBytes
        compressionEnabled = settings.compressionEnabled
        mcpServerEnabled = settings.mcpServerEnabled
        mcpServerPort = settings.mcpServerPort
        // Update flow when settings change
        _settings.value = settings
    }

    // PluginSettingsProvider implementation
    override fun getSettings(): PluginSettings = _settings.value

    override fun updateSettings(settings: PluginSettings) {
        updateFrom(settings)
    }

    companion object {
        fun getInstance(): PluginSettingsState =
            ApplicationManager.getApplication().getService(PluginSettingsState::class.java)
    }
}
