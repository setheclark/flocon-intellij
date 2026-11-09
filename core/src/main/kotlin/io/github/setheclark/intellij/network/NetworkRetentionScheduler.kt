package io.github.setheclark.intellij.network

import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.setheclark.intellij.di.AppCoroutineScope
import io.github.setheclark.intellij.settings.NetworkStorageSettingsProvider
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps network history within the maximum size and age from the settings: once when it starts, whenever either
 * limit changes, and periodically while the IDE runs.
 */
@Inject
@SingleIn(AppScope::class)
class NetworkRetentionScheduler(
    @param:AppCoroutineScope private val scope: CoroutineScope,
    private val dataSource: NetworkDataSource,
    private val settingsProvider: NetworkStorageSettingsProvider,
) {
    private val log = Logger.withPluginTag("NetworkRetentionScheduler")

    fun start(): Job = scope.launch {
        settingsProvider.settings
            .map { it.maxDatabaseSizeBytes to it.maxAgeMillis }
            .distinctUntilChanged()
            .collectLatest { (maxSizeBytes, maxAgeMillis) ->
                while (isActive) {
                    try {
                        dataSource.enforceRetention(maxSizeBytes, maxAgeMillis)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        log.w(t) { "Could not enforce network history retention" }
                    }
                    delay(INTERVAL_MS)
                }
            }
    }

    private companion object {
        const val INTERVAL_MS = 10 * 60 * 1_000L
    }
}
