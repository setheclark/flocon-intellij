package io.github.setheclark.intellij.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkHistoryMode {
    /** Calls and bodies are written to the history database and survive restarts. */
    Persistent,

    /** The history database could not be opened. Calls are kept in memory and lost on restart. */
    MemoryOnly,

    /** The developer turned persistence off. Calls are kept in memory by choice and lost on restart. */
    Disabled,
}

@Inject
@SingleIn(AppScope::class)
class NetworkHistoryStatusDataSource {

    private val _mode = MutableStateFlow(NetworkHistoryMode.Persistent)
    val mode: StateFlow<NetworkHistoryMode> = _mode.asStateFlow()

    fun updateMode(mode: NetworkHistoryMode) {
        _mode.value = mode
    }
}
