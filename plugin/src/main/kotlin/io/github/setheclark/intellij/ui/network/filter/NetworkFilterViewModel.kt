package io.github.setheclark.intellij.ui.network.filter

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.di.ProjectScope
import io.github.setheclark.intellij.ui.network.NetworkInspectorIntent.UpdateFilter
import io.github.setheclark.intellij.ui.network.NetworkInspectorViewModel

@Inject
@SingleIn(ProjectScope::class)
class NetworkFilterViewModel(
    private val parentViewModel: NetworkInspectorViewModel,
) {

    fun dispatch(intent: NetworkFilterIntent) {
        when (intent) {
            is NetworkFilterIntent.UpdateFilter -> {
                parentViewModel.dispatch(UpdateFilter(intent.filter))
            }
        }
    }
}

sealed interface NetworkFilterIntent {
    data class UpdateFilter(val filter: String) : NetworkFilterIntent
}
