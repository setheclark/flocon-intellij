package io.github.setheclark.intellij.domain.models

/**
 * Represents the current state of the WebSocket server.
 */
sealed class ServerState {
    data object Stopped : ServerState()
    data object Starting : ServerState()
    data class Running(val port: Int) : ServerState()
    data object Stopping : ServerState()
    data class Error(val message: String) : ServerState()
}
