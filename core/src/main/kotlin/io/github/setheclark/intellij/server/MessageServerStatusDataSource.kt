package io.github.setheclark.intellij.server

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
class MessageServerStatusDataSource {

    private val _status = MutableStateFlow<MessageServerState>(MessageServerState.Stopped)
    val status: StateFlow<MessageServerState> = _status.asStateFlow()

    fun updateStatus(serverState: MessageServerState) {
        _status.value = serverState
    }
}