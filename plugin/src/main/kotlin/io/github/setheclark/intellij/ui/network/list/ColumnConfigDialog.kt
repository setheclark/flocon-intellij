package io.github.setheclark.intellij.ui.network.list

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import io.github.setheclark.intellij.PluginBundle
import io.github.setheclark.intellij.settings.CallDetailOpenMode
import io.github.setheclark.intellij.settings.NetworkStorageSettingsState
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class ColumnConfigDialog(project: Project) : DialogWrapper(project) {

    private val checkboxes: Map<NetworkCallListColumn, JBCheckBox>
    private val openModeComboBox: JComboBox<CallDetailOpenMode>

    init {
        title = PluginBundle.message("dialog.configureColumns.title")
        val settings = NetworkStorageSettingsState.getInstance()
        checkboxes = NetworkCallListColumn.entries
            .filter { it != NetworkCallListColumn.TIME }
            .associateWith { col ->
                JBCheckBox(col.displayName, col.name !in settings.hiddenColumns)
            }
        openModeComboBox = JComboBox(CallDetailOpenMode.entries.toTypedArray()).apply {
            renderer = SimpleListCellRenderer.create { label, value, _ ->
                label.text = value?.displayName ?: ""
            }
            selectedItem = settings.callDetailOpenMode
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(VerticalLayout(8)).apply {
            border = JBUI.Borders.empty(8)

            add(JBLabel(PluginBundle.message("label.configureColumns.columns")).apply { font = font.deriveFont(Font.BOLD) })
            add(JBCheckBox("Time", true).apply { isEnabled = false })
            checkboxes.values.forEach { add(it) }

            add(JBLabel(PluginBundle.message("label.configureColumns.display")).apply { font = font.deriveFont(Font.BOLD) })
            add(
                JPanel(BorderLayout()).apply {
                    add(JBLabel(PluginBundle.message("label.configureColumns.openCallDetailsIn")), BorderLayout.WEST)
                    add(openModeComboBox, BorderLayout.CENTER)
                },
            )
        }
    }

    override fun doOKAction() {
        val settings = NetworkStorageSettingsState.getInstance()
        settings.hiddenColumns = checkboxes
            .filter { (_, cb) -> !cb.isSelected }
            .keys
            .map { it.name }
            .toMutableSet()
        (openModeComboBox.selectedItem as? CallDetailOpenMode)?.let {
            settings.callDetailOpenMode = it
        }
        super.doOKAction()
    }
}
