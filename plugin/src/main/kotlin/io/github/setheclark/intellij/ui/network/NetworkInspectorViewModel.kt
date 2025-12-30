package io.github.setheclark.intellij.ui.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.setheclark.intellij.adb.AdbStatusDataSource
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.server.usecase.StopMessageServerUseCase
import io.github.setheclark.intellij.ui.network.usecase.ClearAllNetworkCallsUseCase
import io.github.setheclark.intellij.ui.network.usecase.ObserveServerStatusUseCase
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
    private val observeServerStatusUseCase: ObserveServerStatusUseCase,
    private val clearAllNetworkCallsUseCase: ClearAllNetworkCallsUseCase,
    private val startServerUseCase: StartServerUseCase,
    private val stopServerUseCase: StopMessageServerUseCase,
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
                scope.launch {
                    clearAllNetworkCallsUseCase()
                    _state.update {
                        it.copy(selectedCallId = null, autoScrollEnabled = true)
                    }
                }
            }

            is NetworkInspectorIntent.EnableAutoScroll -> {
                _state.update { it.copy(autoScrollEnabled = true) }
            }

            is NetworkInspectorIntent.DisableAutoScroll -> {
                _state.update { it.copy(autoScrollEnabled = false) }
            }


            is NetworkInspectorIntent.StartServer -> {
                scope.launch { startServerUseCase() }
            }

            is NetworkInspectorIntent.StopServer -> {
                scope.launch { stopServerUseCase() }
            }
        }
    }

    private fun observeDataSources() {
        // Observe server state
        scope.launch {
            observeServerStatusUseCase().collect { serverState ->
                _state.update { it.copy(serverState = serverState) }
            }
        }

        // Observe ADB status
        scope.launch {
            adbStatusDataSource.status.collect { status ->
                _state.update { it.copy(adbStatus = status) }
            }
        }
    }
}
