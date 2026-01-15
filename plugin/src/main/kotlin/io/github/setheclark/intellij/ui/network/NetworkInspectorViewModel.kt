package io.github.setheclark.intellij.ui.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdAndPackageNameUseCase
import io.github.openflocon.domain.messages.usecase.StartServerUseCase
import io.github.setheclark.intellij.adb.AdbStatusDataSource
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.di.ProjectScope
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import io.github.setheclark.intellij.server.usecase.StopMessageServerUseCase
import io.github.setheclark.intellij.ui.network.usecase.ClearAllNetworkCallsUseCase
import io.github.setheclark.intellij.ui.network.usecase.ObserveCurrentAppInstanceUseCase
import io.github.setheclark.intellij.ui.network.usecase.ObserveServerStatusUseCase
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@SingleIn(ProjectScope::class)
class NetworkInspectorViewModel(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val adbStatusDataSource: AdbStatusDataSource,
    private val observeServerStatusUseCase: ObserveServerStatusUseCase,
    private val clearAllNetworkCallsUseCase: ClearAllNetworkCallsUseCase,
    private val startServerUseCase: StartServerUseCase,
    private val stopServerUseCase: StopMessageServerUseCase,
    private val observeCurrentDeviceIdAndPackageNameUseCase: ObserveCurrentDeviceIdAndPackageNameUseCase,
    private val observeCurrentAppInstanceUseCase: ObserveCurrentAppInstanceUseCase,
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

        // Observe session changes to reset auto-scroll when a new app session starts
        scope.launch {
            observeCurrentDeviceIdAndPackageNameUseCase()
                .flatMapLatest { deviceAndPackage ->
                    observeCurrentAppInstanceUseCase(deviceAndPackage)
                        .scan<String?, Pair<String?, String?>>(null to null) { (_, prev), current ->
                            prev to current
                        }
                        .filter { (prev, current) ->
                            // Only emit when transitioning from one non-null instance to another
                            prev != null && current != null && prev != current
                        }
                        .map { }
                }
                .collect {
                    log.i { "New session detected, enabling auto-scroll" }
                    _state.update { it.copy(autoScrollEnabled = true) }
                }
        }
    }
}
