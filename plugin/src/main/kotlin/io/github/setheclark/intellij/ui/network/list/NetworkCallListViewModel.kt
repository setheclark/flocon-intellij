package io.github.setheclark.intellij.ui.network.list

import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.device.usecase.ObserveCurrentDeviceIdAndPackageNameUseCase
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.ui.network.NetworkInspectorIntent
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel
import io.github.setheclark.intellij.ui.network.usecase.ObserveFilteredNetworkRequestsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Inject
class NetworkCallListViewModel(
    private val parentViewModel: NetworkInspectorViewModel,
    private val observeNetworkRequestsUseCase: ObserveFilteredNetworkRequestsUseCase,
    private val observeCurrentDeviceIdAndPackageNameUseCase: ObserveCurrentDeviceIdAndPackageNameUseCase,
) {
    private val items: Flow<List<NetworkCallListItem>> =
        combine(
            observeCurrentDeviceIdAndPackageNameUseCase(),
            parentViewModel.state.map { it.filter }.distinctUntilChanged(),
        ) { deviceAndPackage, filter -> deviceAndPackage to filter }
            .flatMapLatest { (deviceIdAndPackageName, filter) ->
                observeNetworkRequestsUseCase(
                    deviceIdAndPackageName = deviceIdAndPackageName,
                    filter = filter,
                ).map { items ->
                    items.map { it.toUi() }
                }
            }

    private val autoScrollEnabled: Flow<Boolean> =
        parentViewModel.state
            .map { it.autoScrollEnabled }
            .distinctUntilChanged()

    val state = combine(
        items,
        autoScrollEnabled,
    ) { items, autoScrollEnabled ->
        NetworkCallListState(
            calls = items,
            autoScrollEnabled = autoScrollEnabled,
        )
    }

    fun dispatch(intent: NetworkCallListIntent) {
        when (intent) {
            NetworkCallListIntent.ClearCallSelection -> {
                parentViewModel.dispatch(NetworkInspectorIntent.SelectCall(null))
            }

            is NetworkCallListIntent.SelectCall -> {
                parentViewModel.dispatch(NetworkInspectorIntent.SelectCall(intent.callId))
            }

            NetworkCallListIntent.DisableAutoScroll -> {
                parentViewModel.dispatch(NetworkInspectorIntent.DisableAutoScroll)
            }
        }
    }

    private fun NetworkCallEntity.toUi(): NetworkCallListItem {
        return NetworkCallListItem(
            callId = callId,
            startTime = startTime,
            name = name,
            url = request.url,
            method = request.method,
            status = (response as? NetworkResponse.Success)?.statusCode,
            duration = response?.durationMs,
            size = (response as? NetworkResponse.Success)?.size
        )
    }
}