package io.github.setheclark.intellij.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.Inject
import io.github.openflocon.domain.common.DispatcherProvider
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.serialization.json.Json

/**
 * Chooses where network history lives: the history database, or memory when the database cannot be opened.
 */
@Inject
class NetworkDataSourceFactory(
    private val location: NetworkHistoryLocation,
    private val status: NetworkHistoryStatusDataSource,
    private val dispatchers: DispatcherProvider,
    private val json: Json,
) {
    private val log = Logger.withPluginTag("NetworkDataSourceFactory")

    fun create(): NetworkDataSource {
        val database = NetworkHistoryDatabaseOpener().open(location.databaseFile)
        if (database == null) {
            log.w { "Network history is memory-only for this session" }
            status.updateMode(NetworkHistoryMode.MemoryOnly)
            return InMemoryNetworkDataSource()
        }
        status.updateMode(NetworkHistoryMode.Persistent)
        return SqlNetworkDataSource(database, json, dispatchers.data)
    }
}
