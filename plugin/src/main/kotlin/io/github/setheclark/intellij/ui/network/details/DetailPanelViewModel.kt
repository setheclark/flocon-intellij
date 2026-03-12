package io.github.setheclark.intellij.ui.network.details

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class DetailPanelViewModel(
    callIdFlow: Flow<String?>,
    observeCallUseCase: ObserveCallUseCase,
) {
    val selectedTabIndex = MutableStateFlow(0)
    val selectedResponseSubTab = MutableStateFlow(0)
    val selectedRequestSubTab = MutableStateFlow(0)

    val selectedCall: Flow<NetworkCallEntity?> = callIdFlow
        .distinctUntilChanged()
        .flatMapLatest { it?.let(observeCallUseCase::invoke) ?: flowOf(null) }
        .distinctUntilChanged()
}
