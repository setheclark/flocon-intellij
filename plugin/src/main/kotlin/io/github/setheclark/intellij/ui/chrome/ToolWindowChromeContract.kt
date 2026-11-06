package io.github.setheclark.intellij.ui.chrome

import androidx.compose.runtime.Immutable
import io.github.setheclark.intellij.adb.AdbStatus
import io.github.setheclark.intellij.server.MessageServerState

/**
 * State of the pieces shared by every feature tab: device/app picker, server controls and warning banner.
 */
@Immutable
data class ToolWindowChromeState(
    val devices: DevicesRenderModel = DevicesRenderModel(emptyList(), -1),
    val serverState: MessageServerState = MessageServerState.Stopped,
    val adbStatus: AdbStatus = AdbStatus.Initializing,
) {
    /** Message for the warning banner, or null when nothing needs the developer's attention. */
    val warning: ChromeWarning?
        get() = when {
            adbStatus == AdbStatus.NotFound -> ChromeWarning.AdbNotFound
            serverState is MessageServerState.Error -> ChromeWarning.ServerError(serverState.message)
            else -> null
        }
}

data class DevicesRenderModel(
    val devices: List<DeviceFilterItem>,
    val selectedIndex: Int,
)

sealed interface ChromeWarning {
    data object AdbNotFound : ChromeWarning
    data class ServerError(val message: String) : ChromeWarning
}

sealed interface ToolWindowChromeIntent {
    data class SelectDevice(val device: DeviceFilterItem) : ToolWindowChromeIntent
    data object StartServer : ToolWindowChromeIntent
    data object StopServer : ToolWindowChromeIntent
}
