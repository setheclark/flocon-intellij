package io.github.setheclark.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import io.github.setheclark.intellij.PluginBundle
import io.github.setheclark.intellij.services.ApplicationService
import javax.swing.JCheckBox

/**
 * Settings page under Tools for network history: whether to persist it, and how large and old it may get.
 */
class NetworkHistoryConfigurable : BoundConfigurable(PluginBundle.message("settings.history.displayName")) {

    private val state: NetworkStorageSettingsState get() = NetworkStorageSettingsState.getInstance()

    override fun createPanel(): DialogPanel = panel {
        lateinit var persist: Cell<JCheckBox>

        row {
            persist = checkBox(PluginBundle.message("settings.history.persist"))
                .bindSelected(state::persistHistory)
        }
        row(PluginBundle.message("settings.history.maxSize")) {
            spinner(MAX_SIZE_MB_RANGE, MAX_SIZE_MB_STEP)
                .bindIntValue(state::maxDatabaseSizeMb)
                .enabledIf(persist.component.selected)
            text(PluginBundle.message("settings.history.maxSize.unit"))
        }
        row(PluginBundle.message("settings.history.maxAge")) {
            spinner(MAX_AGE_DAYS_RANGE, 1)
                .bindIntValue(state::maxAgeDays)
                .enabledIf(persist.component.selected)
            text(PluginBundle.message("settings.history.maxAge.unit"))
        }
        row {
            comment(PluginBundle.message("settings.history.restartNote"))
        }
    }

    override fun apply() {
        super.apply()
        // The provider keeps its own copy of the settings. It only exists once the plugin has started, and reads
        // the saved state when it is first created, so there is nothing to refresh before then.
        ApplicationManager.getApplication()
            .getServiceIfCreated(ApplicationService::class.java)
            ?.appGraph
            ?.networkStorageSettingsProvider
            ?.updateSettings(state.toSettings())
    }

    private companion object {
        val MAX_SIZE_MB_RANGE = 1..1_048_576
        const val MAX_SIZE_MB_STEP = 64
        val MAX_AGE_DAYS_RANGE = 1..3_650
    }
}
