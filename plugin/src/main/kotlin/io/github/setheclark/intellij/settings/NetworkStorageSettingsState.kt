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

    var detailLeftPanelWidthDp: Float = 280f

    var hiddenColumns: MutableSet<String> = mutableSetOf()
    var columnWidths: MutableMap<String, Float> = mutableMapOf()

    override fun getState(): NetworkStorageSettingsState = this

    override fun loadState(state: NetworkStorageSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun toSettings(): NetworkStorageSettings = NetworkStorageSettings(
        hiddenColumns = hiddenColumns.toSet(),
        columnWidths = columnWidths.toMap(),
    )

    fun updateFrom(settings: NetworkStorageSettings) {
        hiddenColumns = settings.hiddenColumns.toMutableSet()
        columnWidths = settings.columnWidths.toMutableMap()
    }

    companion object {
        fun getInstance(): NetworkStorageSettingsState =
            ApplicationManager.getApplication().getService(NetworkStorageSettingsState::class.java)
    }
}
