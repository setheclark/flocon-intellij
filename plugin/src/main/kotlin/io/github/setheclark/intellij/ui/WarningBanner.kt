package io.github.setheclark.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class WarningBanner : JPanel(FlowLayout(FlowLayout.LEFT)) {

    val warningColor = JBColor(0xFFF3CD, 0x5C4813)
    val textColor = JBColor(0x856404, 0xFFE69C)

    val textLabel = JBLabel().apply {
        foreground = textColor
    }

    init {
        background = warningColor
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(8, 12),
        )
        isVisible = false

        val iconLabel = JBLabel(AllIcons.General.Warning)

        add(iconLabel)
        add(textLabel)
    }

    fun setText(text: String) {
        textLabel.text = text
    }
}
