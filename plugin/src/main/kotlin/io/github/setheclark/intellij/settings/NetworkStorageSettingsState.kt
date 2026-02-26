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
    storages = [Storage("flocon-network-storage.xml")],
)
class NetworkStorageSettingsState : PersistentStateComponent<NetworkStorageSettingsState> {

    var callDetailOpenMode: CallDetailOpenMode = CallDetailOpenMode.TOOL_WINDOW_TAB

    var hiddenColumns: MutableSet<String> = mutableSetOf()

    var maxStoredCalls: Int = NetworkStorageSettings.DEFAULT_MAX_STORED_CALLS
    var maxBodyCacheSizeBytes: Long = NetworkStorageSettings.DEFAULT_MAX_BODY_CACHE_SIZE_BYTES
    var maxBodySizeBytes: Int = NetworkStorageSettings.DEFAULT_MAX_BODY_SIZE_BYTES
    var compressionEnabled: Boolean = NetworkStorageSettings.DEFAULT_COMPRESSION_ENABLED

    override fun getState(): NetworkStorageSettingsState = this

    override fun loadState(state: NetworkStorageSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun toSettings(): NetworkStorageSettings = NetworkStorageSettings(
        maxStoredCalls = maxStoredCalls,
        maxBodyCacheSizeBytes = maxBodyCacheSizeBytes,
        maxBodySizeBytes = maxBodySizeBytes,
        compressionEnabled = compressionEnabled,
    )

    fun updateFrom(settings: NetworkStorageSettings) {
        maxStoredCalls = settings.maxStoredCalls
        maxBodyCacheSizeBytes = settings.maxBodyCacheSizeBytes
        maxBodySizeBytes = settings.maxBodySizeBytes
        compressionEnabled = settings.compressionEnabled
    }

    companion object {
        fun getInstance(): NetworkStorageSettingsState =
            ApplicationManager.getApplication().getService(NetworkStorageSettingsState::class.java)
    }
}
