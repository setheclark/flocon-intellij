package io.github.setheclark.intellij.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Settings UI for network storage configuration.
 * Accessible via Settings > Tools > Network Inspector.
 */
class NetworkStorageSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private var maxStoredCallsSpinner: JSpinner? = null
    private var maxBodyCacheSizeMbSpinner: JSpinner? = null
    private var maxBodySizeKbSpinner: JSpinner? = null
    private var compressionEnabledCheckbox: JBCheckBox? = null

    override fun getDisplayName(): String = "Network Inspector"

    override fun createComponent(): JComponent {
        val state = NetworkStorageSettingsState.getInstance()

        maxStoredCallsSpinner = JSpinner(SpinnerNumberModel(
            state.maxStoredCalls,
            100,    // min
            10000,  // max
            100     // step
        ))

        maxBodyCacheSizeMbSpinner = JSpinner(SpinnerNumberModel(
            (state.maxBodyCacheSizeBytes / (1024 * 1024)).toInt(),
            10,     // min MB
            500,    // max MB
            10      // step
        ))

        maxBodySizeKbSpinner = JSpinner(SpinnerNumberModel(
            state.maxBodySizeBytes / 1024,
            0,      // min KB (0 = no truncation)
            10240,  // max KB (10 MB)
            128     // step
        ))

        compressionEnabledCheckbox = JBCheckBox(
            "Enable GZIP compression for stored bodies",
            state.compressionEnabled
        )

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Maximum stored calls:"),
                maxStoredCallsSpinner!!,
                1,
                false
            )
            .addComponentToRightColumn(
                createHintLabel("Oldest calls are removed when this limit is exceeded"),
                0
            )
            .addVerticalGap(8)
            .addLabeledComponent(
                JBLabel("Body cache size (MB):"),
                maxBodyCacheSizeMbSpinner!!,
                1,
                false
            )
            .addComponentToRightColumn(
                createHintLabel("Least recently viewed bodies are removed when exceeded"),
                0
            )
            .addVerticalGap(8)
            .addLabeledComponent(
                JBLabel("Max body size (KB):"),
                maxBodySizeKbSpinner!!,
                1,
                false
            )
            .addComponentToRightColumn(
                createHintLabel("Bodies larger than this are truncated (0 = no limit)"),
                0
            )
            .addVerticalGap(8)
            .addComponent(compressionEnabledCheckbox!!, 0)
            .addComponentToRightColumn(
                createHintLabel("Typically reduces memory usage by 80-90% for JSON/XML"),
                0
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10)
            add(panel, BorderLayout.NORTH)
        }
    }

    private fun createHintLabel(text: String): JBLabel {
        return JBLabel(text).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            font = font.deriveFont(font.size - 1f)
        }
    }

    override fun isModified(): Boolean {
        val state = NetworkStorageSettingsState.getInstance()
        return maxStoredCallsSpinner?.value != state.maxStoredCalls ||
                (maxBodyCacheSizeMbSpinner?.value as? Int)?.times(1024L * 1024) != state.maxBodyCacheSizeBytes ||
                (maxBodySizeKbSpinner?.value as? Int)?.times(1024) != state.maxBodySizeBytes ||
                compressionEnabledCheckbox?.isSelected != state.compressionEnabled
    }

    override fun apply() {
        val state = NetworkStorageSettingsState.getInstance()
        state.maxStoredCalls = maxStoredCallsSpinner?.value as? Int ?: NetworkStorageSettings.DEFAULT_MAX_STORED_CALLS
        state.maxBodyCacheSizeBytes = ((maxBodyCacheSizeMbSpinner?.value as? Int) ?: 50).toLong() * 1024 * 1024
        state.maxBodySizeBytes = ((maxBodySizeKbSpinner?.value as? Int) ?: 1024) * 1024
        state.compressionEnabled = compressionEnabledCheckbox?.isSelected ?: true
    }

    override fun reset() {
        val state = NetworkStorageSettingsState.getInstance()
        maxStoredCallsSpinner?.value = state.maxStoredCalls
        maxBodyCacheSizeMbSpinner?.value = (state.maxBodyCacheSizeBytes / (1024 * 1024)).toInt()
        maxBodySizeKbSpinner?.value = state.maxBodySizeBytes / 1024
        compressionEnabledCheckbox?.isSelected = state.compressionEnabled
    }

    override fun disposeUIResources() {
        panel = null
        maxStoredCallsSpinner = null
        maxBodyCacheSizeMbSpinner = null
        maxBodySizeKbSpinner = null
        compressionEnabledCheckbox = null
    }
}
