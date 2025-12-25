package io.github.setheclark.intellij.domain.models

/**
 * Represents a connected device/app.
 */
data class ConnectedDevice(
    val deviceId: String,
    val deviceName: String,
    val packageName: String,
    val appName: String,
    val appInstance: Long,
)
