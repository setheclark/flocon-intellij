package io.github.setheclark.intellij.ui.toolwindow

import androidx.compose.runtime.Immutable
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.network.NetworkHistoryMode
import io.github.setheclark.intellij.server.MessageServerState

/**
 * State of the pieces shared by every feature tab: device/app picker, server controls and warning banner.
 */
@Immutable
data class ToolWindowState(
    val devices: DevicesRenderModel = DevicesRenderModel(emptyList(), -1),
    val serverState: MessageServerState = MessageServerState.Stopped,
    val adbStatus: AdbStatus = AdbStatus.Initializing,
    val historyMode: NetworkHistoryMode = NetworkHistoryMode.Persistent,
) {
    /** Message for the warning banner, or null when nothing needs the developer's attention. */
    val warning: ToolWindowWarning?
        get() = when {
            adbStatus == AdbStatus.NotFound -> ToolWindowWarning.AdbNotFound
            serverState is MessageServerState.Error -> ToolWindowWarning.ServerError(serverState.message)
            historyMode == NetworkHistoryMode.MemoryOnly -> ToolWindowWarning.HistoryNotPersisted
            else -> null
        }
}

data class DevicesRenderModel(
    val devices: List<DeviceFilterItem>,
    val selectedIndex: Int,
)

sealed interface ToolWindowWarning {
    data object AdbNotFound : ToolWindowWarning
    data class ServerError(val message: String) : ToolWindowWarning
    data object HistoryNotPersisted : ToolWindowWarning
}

sealed interface ToolWindowIntent {
    data class SelectDevice(val device: DeviceFilterItem) : ToolWindowIntent
    data object StartServer : ToolWindowIntent
    data object StopServer : ToolWindowIntent
}
