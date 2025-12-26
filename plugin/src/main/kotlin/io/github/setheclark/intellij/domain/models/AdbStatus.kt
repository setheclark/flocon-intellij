package io.github.setheclark.intellij.domain.models

/**
 * Represents the current status of ADB availability.
 */
sealed class AdbStatus {
    data object Initializing : AdbStatus()
    data class Available(val path: String) : AdbStatus()
    data object NotFound : AdbStatus()
}
