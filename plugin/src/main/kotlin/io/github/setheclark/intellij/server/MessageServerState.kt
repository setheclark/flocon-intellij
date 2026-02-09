package io.github.setheclark.intellij.server

/**
 * Represents the current state of the WebSocket server.
 */
sealed interface MessageServerState {
    data object Starting : MessageServerState
    data object Stopping : MessageServerState
    data object Stopped : MessageServerState
    data object Running : MessageServerState
    data class Error(val message: String) : MessageServerState
}