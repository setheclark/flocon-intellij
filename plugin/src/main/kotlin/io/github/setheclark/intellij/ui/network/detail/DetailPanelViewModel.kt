package io.github.setheclark.intellij.ui.network.detail

import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class DetailPanelViewModel(
    callIdFlow: Flow<String?>,
    observeCallUseCase: ObserveCallUseCase,
) {
    val selectedCall: Flow<NetworkCallEntity?> = callIdFlow
        .distinctUntilChanged()
        .flatMapLatest { it?.let(observeCallUseCase::invoke) ?: flowOf(null) }
        .distinctUntilChanged()
}
