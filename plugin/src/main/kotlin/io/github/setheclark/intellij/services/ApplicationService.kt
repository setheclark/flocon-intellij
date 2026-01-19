package io.github.setheclark.intellij.services

import co.touchlab.kermit.Logger
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import dev.zacsweers.metro.createGraphFactory
import io.github.setheclark.intellij.di.AppGraph
import io.github.setheclark.intellij.settings.PluginSettingsState
import io.github.setheclark.intellij.util.withPluginTag
import kotlinx.coroutines.CoroutineScope

/**
 * Application-level service for Flocon plugin.
 * Thin orchestrator that delegates to injected managers.
 */
@Service(Service.Level.APP)
class ApplicationService(scope: CoroutineScope) : Disposable {

    private val log = Logger.withPluginTag("ApplicationService")

    val appGraph: AppGraph

    init {
        log.i { "initialized" }
        appGraph = createGraphFactory<AppGraph.Factory>().create(
            scope = scope,
            settingsProvider = PluginSettingsState.getInstance(),
        )

        appGraph.applicationServiceDelegate.initialize()
    }

    override fun dispose() {
        log.i { "FloconApplicationService disposing" }
        appGraph.applicationServiceDelegate.shutdown()
    }
}
