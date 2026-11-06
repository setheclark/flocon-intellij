package io.github.setheclark.intellij.network

import com.intellij.openapi.application.PathManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File

/**
 * Keeps network history in the IDE system directory, in a subdirectory named after the plugin id.
 * The system directory is per IDE product and version, so two IDEs never share a database file.
 */
@Inject
@SingleIn(AppScope::class)
class IntellijNetworkHistoryLocation : NetworkHistoryLocation {

    override val databaseFile: File =
        PathManager.getSystemDir().resolve(PLUGIN_ID).resolve("network-history.db").toFile()

    private companion object {
        /** Must match the `<id>` in plugin.xml. */
        const val PLUGIN_ID = "io.github.setheclark.flocon_plugin"
    }
}
