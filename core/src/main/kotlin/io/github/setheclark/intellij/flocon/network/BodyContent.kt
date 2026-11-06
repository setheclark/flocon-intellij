package io.github.setheclark.intellij.flocon.network

/** Result of loading a request or response body of a stored call. */
sealed interface BodyContent {
    /** The full body. */
    data class Present(val text: String) : BodyContent

    /** The call exists but has no body on this side. */
    data object Empty : BodyContent

    /** The call no longer exists, for example because retention purged it. */
    data object Purged : BodyContent
}
