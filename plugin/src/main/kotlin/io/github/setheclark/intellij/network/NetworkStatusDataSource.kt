package io.github.setheclark.intellij.network

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.network.ServerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
class NetworkStatusDataSource {

    private val _status = MutableStateFlow<ServerState>(ServerState.Stopped)
    val status: StateFlow<ServerState> = _status.asStateFlow()

    fun updateStatus(serverState: ServerState) {
        _status.value = serverState
    }
}