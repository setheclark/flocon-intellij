package io.github.setheclark.intellij.ui

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.data.NetworkCallRepository
import io.github.setheclark.intellij.domain.models.NetworkCallEntry
import io.github.setheclark.intellij.domain.models.NetworkFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
class UiStateManager(
    private val networkCallRepository: NetworkCallRepository,
) {

    private val _selectedCall = MutableStateFlow<NetworkCallEntry?>(null)
    val selectedCall: StateFlow<NetworkCallEntry?> = _selectedCall.asStateFlow()

    private val _filter = MutableStateFlow(NetworkFilter())
    val filter: StateFlow<NetworkFilter> = _filter.asStateFlow()

    /**
     * Select a network call to show in the detail panel.
     */
    fun selectCall(call: NetworkCallEntry?) {
        _selectedCall.value = call
    }

    /**
     * Update the filter criteria.
     */
    fun updateFilter(filter: NetworkFilter) {
        _filter.value = filter
    }

    /**
     * Clear all captured network calls.
     */
    fun clearAll() {
        networkCallRepository.clear()
        _selectedCall.value = null
    }
}