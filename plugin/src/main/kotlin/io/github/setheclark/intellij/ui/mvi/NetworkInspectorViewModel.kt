package io.github.setheclark.intellij.ui.mvi

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.Constant
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.managers.adb.AdbManager
import io.github.setheclark.intellij.managers.server.ServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Network Inspector UI.
 *
 * Follows the MVI (Model-View-Intent) pattern:
 * - Maintains a single immutable [NetworkInspectorState]
 * - Processes [NetworkInspectorIntent]s to produce new state
 * - Views observe [state] and call [dispatch] for user actions
 *
 * This centralizes all UI logic and makes the UI components pure renderers.
 */
@Inject
@SingleIn(AppScope::class)
class NetworkInspectorViewModel(
    @param:AppCoroutineScope private val scope: CoroutineScope,
    private val networkCallRepository: NetworkCallRepository,
    private val deviceRepository: DeviceRepository,
    private val serverManager: ServerManager,
    private val adbManager: AdbManager,
) {
    private val _state = MutableStateFlow(NetworkInspectorState())
    val state: StateFlow<NetworkInspectorState> = _state.asStateFlow()

    init {
        observeDataSources()
    }

    /**
     * Dispatch an intent to update state.
     * This is the single entry point for all user actions.
     */
    fun dispatch(intent: NetworkInspectorIntent) {
        when (intent) {
            is NetworkInspectorIntent.SelectCall -> {
                _state.update { it.copy(selectedCall = intent.call) }
            }

            is NetworkInspectorIntent.UpdateFilter -> {
                _state.update { it.copy(filter = intent.filter) }
            }

            is NetworkInspectorIntent.ClearAll -> {
                networkCallRepository.clear()
                _state.update {
                    it.copy(selectedCall = null, autoScrollEnabled = true)
                }
            }

            is NetworkInspectorIntent.EnableAutoScroll -> {
                _state.update { it.copy(autoScrollEnabled = true) }
            }

            is NetworkInspectorIntent.DisableAutoScroll -> {
                _state.update { it.copy(autoScrollEnabled = false) }
            }

            is NetworkInspectorIntent.StartServer -> {
                serverManager.startServer(Constant.SERVER_WEBSOCKET_PORT)
            }

            is NetworkInspectorIntent.StopServer -> {
                serverManager.stopServer()
            }
        }
    }

    private fun observeDataSources() {
        // Observe network calls from repository
        scope.launch {
            networkCallRepository.networkCalls.collect { calls ->
                _state.update { it.copy(calls = calls) }
            }
        }

        // Observe server state
        scope.launch {
            serverManager.serverState.collect { serverState ->
                _state.update { it.copy(serverState = serverState) }
            }
        }

        // Observe ADB status
        scope.launch {
            adbManager.adbStatus.collect { status ->
                _state.update { it.copy(adbStatus = status) }
            }
        }

        // Observe connected devices
        scope.launch {
            deviceRepository.connectedDevices.collect { devices ->
                _state.update { it.copy(connectedDevices = devices) }
            }
        }
    }
}
