package io.github.setheclark.intellij.ui.network.detail.overview

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.flocon.network.NetworkCallEntity
import io.github.setheclark.intellij.flocon.network.NetworkResponse
import io.github.setheclark.intellij.ui.network.detail.common.HeadersTablePanel
import io.github.setheclark.intellij.util.parseQueryParameters
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel

/**
 * Panel displaying overview information about a network call.
 * Contains General, Timing, and Actions sections.
 */
@Inject
class OverviewPanel : JPanel(BorderLayout()) {

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    private val methodLabel = createValueLabel()
    private val nameLabel = createValueLabel()
    private val statusLabel = createValueLabel()
    private val urlLabel = createValueLabel()

    private val durationLabel = createValueLabel()
    private val startTimeLabel = createValueLabel()

    private val queryParametersPanel = HeadersTablePanel()
    private val queryParametersSection = lazy { createQueryParametersSection() }
    private val contentPanel: JPanel

    init {
        contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(12)

            add(createGeneralSection())
            add(Box.createVerticalStrut(16))
            add(createTimingSection())
            add(Box.createVerticalGlue())
        }

        add(JBScrollPane(contentPanel), BorderLayout.CENTER)
    }

    fun showOverview(call: NetworkCallEntity) {
        methodLabel.text = call.request.method
        nameLabel.text = call.name
        urlLabel.text = call.request.url

        val response = call.response
        statusLabel.text = when (response) {
            is NetworkResponse.Success -> buildString {
                response.statusCode?.let { append("$it") }
                response.contentType?.let {
                    if (isNotEmpty()) append(" - ")
                    append(it)
                }
                if (isEmpty()) append("Success")
            }

            is NetworkResponse.Failure -> "Error: ${response.issue}"

            null -> "Pending"
        }

        durationLabel.text = response?.durationMs?.let { "${it}ms" } ?: "N/A"
        startTimeLabel.text = timeFormatter.format(Instant.ofEpochMilli(call.startTime))

        // Parse and display query parameters
        val queryParams = parseQueryParameters(call.request.url)
        queryParametersPanel.showHeaders(queryParams)

        // Conditionally show/hide Query Parameters section
        val section = queryParametersSection.value
        if (queryParams.isNotEmpty()) {
            // Add section if not already present
            if (section.parent == null) {
                val components = contentPanel.components
                val generalSectionIndex = components.indexOfFirst { it is JPanel }
                if (generalSectionIndex >= 0) {
                    // Insert after General section and its strut
                    val insertIndex = generalSectionIndex + 2
                    contentPanel.add(section, insertIndex)
                    contentPanel.add(Box.createVerticalStrut(16), insertIndex + 1)
                }
            }
        } else {
            // Remove section if present
            if (section.parent != null) {
                val index = contentPanel.components.indexOf(section)
                if (index >= 0) {
                    contentPanel.remove(section)
                    // Remove the strut after the section if it exists
                    if (index < contentPanel.componentCount) {
                        val nextComponent = contentPanel.getComponent(index)
                        if (nextComponent is Box.Filler) {
                            contentPanel.remove(nextComponent)
                        }
                    }
                }
            }
        }

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun createGeneralSection(): JPanel {
        return createSection("General") {
            add(createRow("Method", methodLabel))
            add(createRow("Name", nameLabel))
            add(createRow("Status", statusLabel))
            add(createRow("URL", urlLabel))
        }
    }

    private fun createTimingSection(): JPanel {
        return createSection("Timing") {
            add(createRow("Duration", durationLabel))
            add(createRow("Start Time", startTimeLabel))
        }
    }

    private fun createQueryParametersSection(): JPanel {
        return createSection("Query Parameters") {
            add(
                queryParametersPanel.apply {
                    alignmentX = LEFT_ALIGNMENT
                },
            )
        }
    }

    private fun createSection(title: String, buildContent: JPanel.() -> Unit): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            alignmentX = LEFT_ALIGNMENT

            add(
                JBLabel(title).apply {
                    font = font.deriveFont(Font.BOLD, 14f)
                    border = JBUI.Borders.emptyBottom(8)
                    alignmentX = LEFT_ALIGNMENT
                },
            )

            buildContent()
        }
    }

    private fun createRow(label: String, valueComponent: JBLabel): JPanel {
        return JPanel(BorderLayout()).apply {
            alignmentX = LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, JBUI.scale(24))
            border = JBUI.Borders.emptyBottom(4)

            add(
                JBLabel("$label:").apply {
                    foreground = JBColor.GRAY
                    preferredSize = Dimension(JBUI.scale(100), preferredSize.height)
                },
                BorderLayout.WEST,
            )

            add(valueComponent, BorderLayout.CENTER)
        }
    }

    private fun createValueLabel(): JBLabel {
        return JBLabel().apply {
            setCopyable(true)
        }
    }
}
