package io.github.setheclark.intellij.ui.network.list

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import io.github.setheclark.intellij.settings.NetworkStorageSettingsState
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel

class ColumnConfigDialog(project: Project) : DialogWrapper(project) {

    private val checkboxes: Map<NetworkCallListColumn, JBCheckBox>

    init {
        title = "Configure Columns"
        val settings = NetworkStorageSettingsState.getInstance()
        checkboxes = NetworkCallListColumn.entries
            .filter { it != NetworkCallListColumn.TIME }
            .associateWith { col ->
                JBCheckBox(col.displayName, col.name !in settings.hiddenColumns)
            }
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(VerticalLayout(8)).apply {
            border = JBUI.Borders.empty(8)
            add(JBLabel("Columns").apply { font = font.deriveFont(Font.BOLD) })
            add(JBCheckBox("Time", true).apply { isEnabled = false })
            checkboxes.values.forEach { add(it) }
        }
    }

    override fun doOKAction() {
        val settings = NetworkStorageSettingsState.getInstance()
        settings.hiddenColumns = checkboxes
            .filter { (_, cb) -> !cb.isSelected }
            .keys
            .map { it.name }
            .toMutableSet()
        super.doOKAction()
    }
}
