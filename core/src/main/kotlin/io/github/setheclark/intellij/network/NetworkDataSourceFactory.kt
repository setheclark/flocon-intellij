package io.github.setheclark.intellij.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProvider
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.serialization.json.Json

/**
 * Chooses where network history lives: the history database, or memory when persistence is turned off or the
 * database cannot be opened. The choice is made once, so a persistence change applies after a restart.
 */
@Inject
class NetworkDataSourceFactory(
    private val location: NetworkHistoryLocation,
    private val status: NetworkHistoryStatusDataSource,
    private val settingsProvider: NetworkStorageSettingsProvider,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) {
    private val log = Logger.withPluginTag("NetworkDataSourceFactory")

    fun create(): NetworkDataSource {
        if (!settingsProvider.getSettings().persistHistory) {
            log.i { "Network history persistence is turned off, history is memory-only" }
            status.updateMode(NetworkHistoryMode.Disabled)
            return InMemoryNetworkDataSource()
        }

        val opened = NetworkHistoryDatabaseOpener().open(location.databaseFile)
        if (opened == null) {
            log.w { "Network history is memory-only for this session" }
            status.updateMode(NetworkHistoryMode.MemoryOnly)
            return InMemoryNetworkDataSource()
        }
        status.updateMode(NetworkHistoryMode.Persistent)
        return SqlNetworkDataSource(opened.database, opened.driver, opened.dataSource, json, dispatchers.data)
    }
}
