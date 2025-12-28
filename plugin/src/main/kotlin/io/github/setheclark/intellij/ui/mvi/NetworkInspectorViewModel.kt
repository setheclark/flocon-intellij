package io.github.setheclark.intellij.ui.mvi

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.openflocon.domain.Constant
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdAndPackageNameUseCase
import io.github.openflocon.domain.network.models.FloconNetworkCallDomainModel
import io.github.openflocon.domain.network.models.NetworkFilterDomainModel
import io.github.setheclark.intellij.adb.AdbStatusManager
import io.github.setheclark.intellij.data.DeviceRepository
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.managers.server.ServerManager
import io.github.setheclark.intellij.network.usecase.ObserveNetworkRequestsUseCase
import io.github.setheclark.intellij.ui.list.NetworkCallListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
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
    private val adbStatusManager: AdbStatusManager,
    private val observeNetworkRequestsUseCase: ObserveNetworkRequestsUseCase,
    private val observeCurrentDeviceIdAndPackageNameUseCase: ObserveCurrentDeviceIdAndPackageNameUseCase,
) {
    private val _state = MutableStateFlow(NetworkInspectorState())
    val state: StateFlow<NetworkInspectorState> = _state.asStateFlow()

    init {
        observeDataSources()
    }

    val items: Flow<List<NetworkCallListItem>> =
        observeCurrentDeviceIdAndPackageNameUseCase()
            .flatMapLatest { deviceIdAndPackageName ->
                observeNetworkRequestsUseCase(
                    sortedBy = null,
                    filter = NetworkFilterDomainModel(
                        null, null, null, false,
                    ),
                    deviceIdAndPackageName = deviceIdAndPackageName,
                ).map { items ->
                    items.map { it.toUi() }
                }
            }

    private fun FloconNetworkCallDomainModel.toUi(): NetworkCallListItem {
        return NetworkCallListItem(
            callId = callId,
            startTime = request.startTimeFormatted,
            url = request.url,
            method = request.method,
            status = (response as? FloconNetworkCallDomainModel.Response.Success)?.statusFormatted,
            duration = response?.durationFormatted,
            size = (response as? FloconNetworkCallDomainModel.Response.Success)?.byteSizeFormatted
        )
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
            adbStatusManager.status.collect { status ->
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
