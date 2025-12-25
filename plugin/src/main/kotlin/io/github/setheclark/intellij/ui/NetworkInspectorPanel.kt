package io.github.setheclark.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.domain.models.ConnectedDevice
import io.github.setheclark.intellij.domain.models.NetworkFilter
import io.github.setheclark.intellij.domain.models.ServerState
import io.github.setheclark.intellij.domain.models.StatusFilter
import io.github.setheclark.intellij.domain.models.AdbStatus
import io.github.setheclark.intellij.managers.adb.AdbManager
import io.github.setheclark.intellij.services.FloconApplicationService
import io.github.setheclark.intellij.services.FloconProjectService
import io.github.setheclark.intellij.ui.detail.DetailPanel
import io.github.setheclark.intellij.ui.list.NetworkCallListPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

/**
 * Main panel for the Flocon Network Inspector tool window.
 * Contains the toolbar, network call list, and detail panel.
 */
@Inject
class NetworkInspectorPanel(
    private val floconService: FloconProjectService,
    private val appService: FloconApplicationService,
    private val adbManager: AdbManager,
    private val networkCallListPanel: NetworkCallListPanel,
    private val detailPanel: DetailPanel,
) : SimpleToolWindowPanel(true, true), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val statusLabel = JBLabel()
    private val warningBanner = createWarningBanner()
    private var mainSplitter: JBSplitter

    // Filter components (integrated into toolbar)
    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = "Filter requests..."
        preferredSize = JBUI.size(200, 28)
    }
    private val deviceComboBox = JComboBox<DeviceFilterItem>().apply {
        addItem(DeviceFilterItem("All Devices", null, null))
    }

    init {
        // Create combined toolbar with actions and filters
        val toolbarPanel = createToolbarPanel()
        setToolbar(toolbarPanel)

        // Setup filter listeners
        setupFilterListeners()

        // Observe connected devices for filter dropdown
        observeConnectedDevices()

        // Create main content with split pane (horizontal: list on left, details on right)
        // Detail panel gets full vertical height for easier reading
        mainSplitter = JBSplitter(false, 0.35f).apply {
            firstComponent = networkCallListPanel
            secondComponent = null  // Hidden initially until a request is selected
            setHonorComponentsMinimumSize(true)
        }

        // Add status bar at bottom
        val contentPanel = JPanel(BorderLayout()).apply {
            add(warningBanner, BorderLayout.NORTH)
            add(mainSplitter, BorderLayout.CENTER)
            add(createStatusBar(), BorderLayout.SOUTH)
        }

        setContent(contentPanel)

        // Observe server state for status bar
        observeServerState()
        // Observe ADB status for warning banner
        observeAdbStatus()
        // Observe selected call to show/hide detail panel
        observeSelectedCall()
    }

    private fun createToolbarPanel(): JPanel {
        val actionGroup = DefaultActionGroup().apply {
            add(ClearAction())
            addSeparator()
            add(AutoScrollAction())
            addSeparator()
            add(StartStopServerAction())
        }

        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("FloconNetworkToolbar", actionGroup, true)
            .apply {
                targetComponent = this@NetworkInspectorPanel
            }

        // Create filter panel with search and device filter
        val filterPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            border = JBUI.Borders.empty()
            add(searchField)
            add(deviceComboBox)
        }

        // Combine action toolbar and filter panel
        return JPanel(BorderLayout()).apply {
            add(actionToolbar.component, BorderLayout.WEST)
            add(filterPanel, BorderLayout.CENTER)
        }
    }

    private fun setupFilterListeners() {
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateFilter()
            }
        })

        deviceComboBox.addActionListener { updateFilter() }
    }

    private fun updateFilter() {
        val searchText = searchField.text.trim()
        val deviceItem = deviceComboBox.selectedItem as? DeviceFilterItem
        val deviceFilter = deviceItem?.deviceId

        floconService.updateFilter(
            NetworkFilter(
                searchText = searchText,
                methodFilter = null,
                statusFilter = StatusFilter.ALL,
                deviceFilter = deviceFilter,
            )
        )
    }

    private fun observeConnectedDevices() {
        scope.launch {
            appService.connectedDevices.collectLatest { devices ->
                SwingUtilities.invokeLater {
                    updateDeviceComboBox(devices)
                }
            }
        }
    }

    private fun updateDeviceComboBox(devices: Set<ConnectedDevice>) {
        val currentSelection = deviceComboBox.selectedItem as? DeviceFilterItem
        val model = DefaultComboBoxModel<DeviceFilterItem>()

        model.addElement(DeviceFilterItem("All Devices", null, null))

        devices.forEach { device ->
            val displayName = buildString {
                if (device.deviceName.isNotEmpty()) {
                    append(device.deviceName)
                } else {
                    append(device.deviceId.take(12))
                }
                append(" - ")
                if (device.appName.isNotEmpty()) {
                    append(device.appName)
                } else {
                    append(device.packageName.substringAfterLast('.'))
                }
            }
            model.addElement(DeviceFilterItem(displayName, device.deviceId, device.packageName))
        }

        deviceComboBox.model = model

        // Restore selection if device still connected
        if (currentSelection != null && currentSelection.deviceId != null) {
            for (i in 0 until model.size) {
                val item = model.getElementAt(i)
                if (item.deviceId == currentSelection.deviceId &&
                    item.packageName == currentSelection.packageName
                ) {
                    deviceComboBox.selectedIndex = i
                    break
                }
            }
        }
    }

    private fun createStatusBar(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2, 8)
            add(statusLabel, BorderLayout.WEST)
        }
    }

    private fun createWarningBanner(): JPanel {
        val warningColor = JBColor(0xFFF3CD, 0x5C4813) // Yellow warning background
        val textColor = JBColor(0x856404, 0xFFE69C)    // Dark text on light, light text on dark

        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = warningColor
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(8, 12)
            )
            isVisible = false // Hidden by default

            val iconLabel = JBLabel(AllIcons.General.Warning)
            val textLabel = JBLabel().apply {
                foreground = textColor
            }

            add(iconLabel)
            add(textLabel)

            // Store text label for updates
            putClientProperty("textLabel", textLabel)
        }
    }

    private fun observeAdbStatus() {
        scope.launch {
            adbManager.adbStatus.collectLatest { status ->
                SwingUtilities.invokeLater {
                    updateWarningBanner(status)
                }
            }
        }
    }

    private fun updateWarningBanner(status: AdbStatus) {
        val textLabel = warningBanner.getClientProperty("textLabel") as? JBLabel
        when (status) {
            is AdbStatus.NotFound -> {
                textLabel?.text = "<html><b>ADB not found.</b> USB device connections won't work. " +
                        "Add 'adb' to PATH or set ANDROID_HOME environment variable.</html>"
                warningBanner.isVisible = true
            }

            else -> {
                warningBanner.isVisible = false
            }
        }
        warningBanner.revalidate()
        warningBanner.repaint()
    }

    private fun observeSelectedCall() {
        scope.launch {
            floconService.selectedCall.collectLatest { call ->
                SwingUtilities.invokeLater {
                    if (call != null) {
                        // Show detail panel when a call is selected
                        if (mainSplitter.secondComponent == null) {
                            mainSplitter.secondComponent = detailPanel
                        }
                    } else {
                        // Hide detail panel when no call is selected
                        mainSplitter.secondComponent = null
                    }
                }
            }
        }
    }

    private fun observeServerState() {
        scope.launch {
            floconService.serverState.collectLatest { state ->
                SwingUtilities.invokeLater {
                    updateStatusLabel(state)
                }
            }
        }
    }

    private fun updateStatusLabel(state: ServerState) {
        statusLabel.text = when (state) {
            is ServerState.Stopped -> "Server stopped"
            is ServerState.Starting -> "Starting server..."
            is ServerState.Running -> "Server running on port ${state.port}"
            is ServerState.Stopping -> "Stopping server..."
            is ServerState.Error -> "Error: ${state.message}"
        }
    }

    override fun dispose() {
        scope.cancel()
    }

    // Action implementations

    private inner class ClearAction : AnAction(
        "Clear All",
        "Clear all captured network traffic",
        AllIcons.Actions.GC
    ) {
        override fun actionPerformed(e: AnActionEvent) {
            floconService.clearAll()
            networkCallListPanel.resetAutoScroll()
        }

        override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }

    private inner class AutoScrollAction : ToggleAction(
        "Auto-scroll to Latest",
        "Automatically scroll to show new requests",
        AllIcons.RunConfigurations.Scroll_down
    ) {
        override fun isSelected(e: AnActionEvent): Boolean {
            return networkCallListPanel.isAutoScrollEnabled()
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                networkCallListPanel.enableAutoScroll()
            }
            // When toggled off, we don't need to do anything -
            // user interaction already disables auto-scroll
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private inner class StartStopServerAction : AnAction() {
        override fun update(e: AnActionEvent) {
            val state = floconService.serverState.value
            when (state) {
                is ServerState.Running -> {
                    e.presentation.text = "Stop Server"
                    e.presentation.description = "Stop the Flocon server"
                    e.presentation.icon = AllIcons.Actions.Suspend
                    e.presentation.isEnabled = true
                }

                is ServerState.Stopped -> {
                    e.presentation.text = "Start Server"
                    e.presentation.description = "Start the Flocon server"
                    e.presentation.icon = AllIcons.Actions.Execute
                    e.presentation.isEnabled = true
                }

                is ServerState.Error -> {
                    e.presentation.text = "Retry Server"
                    e.presentation.description = "Retry starting the Flocon server"
                    e.presentation.icon = AllIcons.Actions.Restart
                    e.presentation.isEnabled = true
                }

                else -> {
                    e.presentation.isEnabled = false
                }
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val appService = service<FloconApplicationService>()
            when (floconService.serverState.value) {
                is ServerState.Running -> appService.stopServer()
                is ServerState.Stopped, is ServerState.Error -> appService.startServer()
                else -> { /* ignore */
                }
            }
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
}

/**
 * Combo box item for device filter.
 */
private data class DeviceFilterItem(
    val displayName: String,
    val deviceId: String?,
    val packageName: String?
) {
    override fun toString(): String = displayName
}
