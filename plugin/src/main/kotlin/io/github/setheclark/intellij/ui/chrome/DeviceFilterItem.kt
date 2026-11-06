package io.github.setheclark.intellij.ui.chrome

private const val MAX_DEVICE_NAME_LENGTH = 28

/**
 * Combo box item for device filter.
 */
data class DeviceFilterItem(
    val displayName: String,
    val packageName: String,
    val deviceId: String,
    val appName: String = "",
) {
    /** Stable identity; the display text is not unique. */
    val key: String get() = "$deviceId|$packageName"

    /** App label, falling back to the last package segment when Flocon gives no name. */
    val appLabel: String
        get() = appName.ifBlank { packageName.substringAfterLast('.') }

    /** Short single-line label: the device is truncated so the app always stays visible. */
    val shortLabel: String
        get() {
            val device = if (displayName.length > MAX_DEVICE_NAME_LENGTH) {
                displayName.take(MAX_DEVICE_NAME_LENGTH - 1).trimEnd() + "…"
            } else {
                displayName
            }
            return "$device · $appLabel"
        }

    /** Full text for tooltips. */
    val fullLabel: String get() = "$displayName · $appLabel · $packageName"

    override fun toString(): String = shortLabel
}
