package io.github.setheclark.intellij.ui.network.filter

/**
 * Combo box item for device filter.
 */
data class DeviceFilterItem(
    val displayName: String,
    val packageName: String,
    val deviceId: String,
) {
    override fun toString(): String = displayName
}