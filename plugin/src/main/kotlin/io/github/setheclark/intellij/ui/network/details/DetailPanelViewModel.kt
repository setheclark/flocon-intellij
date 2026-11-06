package io.github.setheclark.intellij.ui.network.details

import io.github.setheclark.intellij.flocon.network.BodyContent
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.network.BodyType
import io.github.setheclark.intellij.ui.network.usecase.GetCallBodyUseCase
import io.github.setheclark.intellij.ui.network.usecase.ObserveCallUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class DetailPanelViewModel(
    callIdFlow: Flow<String?>,
    observeCallUseCase: ObserveCallUseCase,
    private val getCallBodyUseCase: GetCallBodyUseCase,
    val layoutMode: DetailLayoutMode = DetailLayoutMode.ToolWindow,
) {
    val selectedCall: Flow<NetworkCallEntity?> = callIdFlow
        .distinctUntilChanged()
        .flatMapLatest { it?.let(observeCallUseCase::invoke) ?: flowOf(null) }
        .distinctUntilChanged()

    /** Loads a body when a tab that shows it is opened. */
    suspend fun loadBody(callId: String, type: BodyType): BodyContent = getCallBodyUseCase(callId, type)

    /** Which bodies exist for a call, without reading their text. */
    suspend fun loadBodyTypes(callId: String): Set<BodyType> = getCallBodyUseCase.available(callId)
}
