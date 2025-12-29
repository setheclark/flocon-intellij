package io.github.setheclark.intellij.ui.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.adb.AdbStatusDataSource
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
class NetworkInspectorViewModel(
    @param:AppCoroutineScope private val scope: CoroutineScope,
    private val adbStatusDataSource: AdbStatusDataSource,
) {
    private val log = Logger.withPluginTag("NetworkInspectorViewModel")

    private val _state = MutableStateFlow(NetworkInspectorState())
    val state: StateFlow<NetworkInspectorState> = _state.asStateFlow()

    init {
        observeDataSources()
    }

    fun dispatch(intent: NetworkInspectorIntent) {
        when (intent) {
            is NetworkInspectorIntent.SelectCall -> {
                log.w { "Call selected: ${intent.callId}" }
                _state.update { it.copy(selectedCallId = intent.callId) }
            }

            is NetworkInspectorIntent.UpdateFilter -> {
                _state.update { it.copy(filter = it.filter.copy(searchText = intent.filter)) }
            }

            is NetworkInspectorIntent.ClearAll -> {
//                networkCallRepository.clear()
//                _state.update {
//                    it.copy(selectedCall = null, autoScrollEnabled = true)
//                }
            }

            is NetworkInspectorIntent.EnableAutoScroll -> {
                _state.update { it.copy(autoScrollEnabled = true) }
            }

            is NetworkInspectorIntent.DisableAutoScroll -> {
                _state.update { it.copy(autoScrollEnabled = false) }
            }

            is NetworkInspectorIntent.StartServer -> {
//                serverManager.startServer(Constant.SERVER_WEBSOCKET_PORT)
            }

            is NetworkInspectorIntent.StopServer -> {
//                serverManager.stopServer()
            }
        }
    }

    private fun observeDataSources() {
        // Observe network calls from repository

        // Observe server state
//        scope.launch {
//            serverManager.serverState.collect { serverState ->
//                _state.update { it.copy(serverState = serverState) }
//            }
//        }

        // Observe ADB status
        scope.launch {
            adbStatusDataSource.status.collect { status ->
                _state.update { it.copy(adbStatus = status) }
            }
        }
    }
}
