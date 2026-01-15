package io.github.setheclark.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Persistent state component for network storage settings.
 * Settings are stored in the IDE's configuration directory.
 */
@Service(Service.Level.APP)
@State(
    name = "FloconNetworkStorageSettings",
    storages = [Storage("flocon-network-storage.xml")]
)
class NetworkStorageSettingsState : PersistentStateComponent<NetworkStorageSettingsState> {

    var maxStoredCalls: Int = NetworkStorageSettings.DEFAULT_MAX_STORED_CALLS
    var maxBodyCacheSizeBytes: Long = NetworkStorageSettings.DEFAULT_MAX_BODY_CACHE_SIZE_BYTES
    var maxBodySizeBytes: Int = NetworkStorageSettings.DEFAULT_MAX_BODY_SIZE_BYTES
    var compressionEnabled: Boolean = NetworkStorageSettings.DEFAULT_COMPRESSION_ENABLED
    var mcpServerEnabled: Boolean = NetworkStorageSettings.DEFAULT_MCP_SERVER_ENABLED
    var mcpServerPort: Int = NetworkStorageSettings.DEFAULT_MCP_SERVER_PORT

    override fun getState(): NetworkStorageSettingsState = this

    override fun loadState(state: NetworkStorageSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun toSettings(): NetworkStorageSettings = NetworkStorageSettings(
        maxStoredCalls = maxStoredCalls,
        maxBodyCacheSizeBytes = maxBodyCacheSizeBytes,
        maxBodySizeBytes = maxBodySizeBytes,
        compressionEnabled = compressionEnabled,
        mcpServerEnabled = mcpServerEnabled,
        mcpServerPort = mcpServerPort,
    )

    fun updateFrom(settings: NetworkStorageSettings) {
        maxStoredCalls = settings.maxStoredCalls
        maxBodyCacheSizeBytes = settings.maxBodyCacheSizeBytes
        maxBodySizeBytes = settings.maxBodySizeBytes
        compressionEnabled = settings.compressionEnabled
        mcpServerEnabled = settings.mcpServerEnabled
        mcpServerPort = settings.mcpServerPort
    }

    companion object {
        fun getInstance(): NetworkStorageSettingsState =
            ApplicationManager.getApplication().getService(NetworkStorageSettingsState::class.java)
    }
}
