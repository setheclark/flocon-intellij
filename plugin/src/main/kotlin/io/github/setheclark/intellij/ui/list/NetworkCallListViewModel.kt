package io.github.setheclark.intellij.ui.list

import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.ui.mvi.NetworkInspectorIntent
import io.github.setheclark.intellij.ui.mvi.NetworkInspectorViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * ViewModel for [NetworkCallListPanel].
 *
 * Selects relevant state from [NetworkInspectorViewModel] and exposes
 * a focused API for the list panel. Delegates actions back to the parent.
 *
 * Uses plain [Flow] instead of [StateFlow] since:
 * - The parent's state is already a StateFlow (shared, cached)
 * - Only one subscriber (the panel) collects these flows
 * - No synchronous `.value` access is needed
 * - No scope/lifecycle management required
 */
@Inject
class NetworkCallListViewModel(
    private val parentViewModel: NetworkInspectorViewModel,
) {
    /**
     * Filtered network calls to display in the list.
     */
    val filteredCalls: Flow<List<NetworkCallEntry>> =
        parentViewModel.state
            .map { it.filteredCalls }
            .distinctUntilChanged()

    /**
     * Whether auto-scroll is enabled for new entries.
     */
    val autoScrollEnabled: Flow<Boolean> =
        parentViewModel.state
            .map { it.autoScrollEnabled }
            .distinctUntilChanged()

    /**
     * Select a network call to view details.
     */
    fun selectCall(call: NetworkCallEntry?) {
        parentViewModel.dispatch(NetworkInspectorIntent.SelectCall(call))
    }

    /**
     * Disable auto-scroll (e.g., when user manually scrolls or selects).
     */
    fun disableAutoScroll() {
        parentViewModel.dispatch(NetworkInspectorIntent.DisableAutoScroll)
    }
}
