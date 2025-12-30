package io.github.setheclark.intellij.ui.network.detail.common

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.JBColor
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

/**
 * Reusable panel displaying HTTP headers in a borderless, headerless two-column table.
 * Columns: Header Name | Header Value
 *
 * Supports copying via right-click context menu and Cmd/Ctrl+C keyboard shortcut.
 */
class HeadersTablePanel : JPanel(BorderLayout()) {

    private val tableModel = HeadersTableModel()
    private val table = JBTable(tableModel).apply {
        tableHeader = null
        setShowGrid(false)
        showHorizontalLines = false
        showVerticalLines = false
        intercellSpacing = JBUI.emptySize()
        rowHeight = JBUI.scale(24)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    private val emptyLabel = JBLabel("No headers").apply {
        horizontalAlignment = JBLabel.CENTER
        foreground = JBColor.GRAY
    }

    init {
        setupCopySupport()
        showEmpty()
    }

    fun showHeaders(headers: Map<String, String>?) {
        if (headers.isNullOrEmpty()) {
            showEmpty()
        } else {
            tableModel.updateHeaders(headers)
            showTable()
        }
    }

    private fun setupCopySupport() {
        // Keyboard shortcut: Cmd+C (Mac) or Ctrl+C (Windows/Linux)
        val copyKeyStroke = KeyStroke.getKeyStroke(
            KeyEvent.VK_C,
            Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        )
        table.inputMap.put(copyKeyStroke, "copy")
        table.actionMap.put("copy", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                copySelectedValue()
            }
        })

        // Right-click context menu using IntelliJ's action system
        val actionGroup = DefaultActionGroup().apply {
            add(CopyValueAction())
            add(CopyNameAction())
            add(CopyNameValueAction())
        }

        PopupHandler.installPopupMenu(table, actionGroup, "HeadersTablePopup")
    }

    private fun getSelectedHeader(): Pair<String, String>? {
        val row = table.selectedRow
        return if (row >= 0) tableModel.getHeader(row) else null
    }

    private fun copySelectedValue() {
        val (_, value) = getSelectedHeader() ?: return
        copyToClipboard(value)
    }

    private fun copyToClipboard(text: String) {
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun showEmpty() {
        removeAll()
        add(emptyLabel, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun showTable() {
        removeAll()
        add(JBScrollPane(table), BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private inner class CopyValueAction : AnAction("Copy Value") {
        override fun actionPerformed(e: AnActionEvent) {
            val (_, value) = getSelectedHeader() ?: return
            copyToClipboard(value)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedHeader() != null
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class CopyNameAction : AnAction("Copy Name") {
        override fun actionPerformed(e: AnActionEvent) {
            val (name, _) = getSelectedHeader() ?: return
            copyToClipboard(name)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedHeader() != null
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class CopyNameValueAction : AnAction("Copy Name: Value") {
        override fun actionPerformed(e: AnActionEvent) {
            val (name, value) = getSelectedHeader() ?: return
            copyToClipboard("$name: $value")
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = getSelectedHeader() != null
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}

private class HeadersTableModel : AbstractTableModel() {

    private var headers: List<Pair<String, String>> = emptyList()

    fun updateHeaders(newHeaders: Map<String, String>) {
        headers = newHeaders.entries
            .sortedBy { it.key.lowercase() }
            .map { it.key to it.value }
        fireTableDataChanged()
    }

    fun getHeader(row: Int): Pair<String, String>? = headers.getOrNull(row)

    override fun getRowCount(): Int = headers.size

    override fun getColumnCount(): Int = 2

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val (name, value) = headers.getOrNull(rowIndex) ?: return null
        return if (columnIndex == 0) name else value
    }
}
