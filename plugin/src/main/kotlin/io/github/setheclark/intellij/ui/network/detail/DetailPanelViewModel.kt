package io.github.setheclark.intellij.ui.network.detail

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull

@Inject
class DetailPanelViewModel(
    parentViewModel: NetworkInspectorViewModel,
    observeCallUseCase: ObserveCallUseCase,
) {
    val selectedCall: Flow<NetworkCallEntity> = parentViewModel.state
        .mapNotNull { it.selectedCallId }
        .distinctUntilChanged()
        .flatMapLatest { observeCallUseCase.invoke(it) }
}
