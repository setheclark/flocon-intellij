package io.github.setheclark.intellij.ui.detail

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.ui.mvi.NetworkInspectorViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * ViewModel for [DetailPanel].
 *
 * Selects the selected call from [NetworkInspectorViewModel] and exposes
 * a focused API for the detail panel.
 *
 * Uses plain [Flow] instead of [StateFlow] since:
 * - The parent's state is already a StateFlow (shared, cached)
 * - Only one subscriber (the panel) collects this flow
 * - No synchronous `.value` access is needed
 * - No scope/lifecycle management required
 */
@Inject
class DetailPanelViewModel(
    parentViewModel: NetworkInspectorViewModel,
) {
    /**
     * The currently selected network call to display details for.
     * Null when no call is selected.
     */
    val selectedCall: Flow<NetworkCallEntry?> =
        parentViewModel.state
            .map { it.selectedCall }
            .distinctUntilChanged()
}
