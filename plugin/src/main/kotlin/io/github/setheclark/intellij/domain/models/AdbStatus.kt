package io.github.setheclark.intellij.domain.models

/**
 * Represents the current status of ADB availability.
 */
sealed class AdbStatus {
    data object Initializing : AdbStatus()
    data class Available(val path: String) : AdbStatus()
    data object NotFound : AdbStatus() {
        val message: String = "ADB not found. USB device connections require ADB.\n\n" +
                "To fix this, either:\n" +
                "• Add 'adb' to your system PATH\n" +
                "• Set ANDROID_HOME or ANDROID_SDK_ROOT environment variable\n" +
                "• Install Android SDK in ~/Library/Android/sdk (macOS)\n\n" +
                "WiFi connections will still work if the device can reach this computer."
    }

    data object Forwarding : AdbStatus()
}
