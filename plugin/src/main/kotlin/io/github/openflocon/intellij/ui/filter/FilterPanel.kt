package io.github.openflocon.intellij.ui.filter

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.openflocon.intellij.services.ConnectedDevice
import io.github.openflocon.intellij.services.FloconApplicationService
import io.github.openflocon.intellij.services.FloconProjectService
import io.github.openflocon.intellij.services.NetworkFilter
import io.github.openflocon.intellij.services.StatusFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent

/**
 * Panel containing filter controls for network traffic.
 * Includes search field, method filter, status filter, and device filter.
 */
class FilterPanel(
    private val project: Project
) : JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val floconService = project.service<FloconProjectService>()
    private val appService = service<FloconApplicationService>()

    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = "Filter by URL..."
        preferredSize = JBUI.size(200, 28)
    }

    private val methodComboBox = JComboBox(arrayOf(
        "All Methods",
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "PATCH",
        "HEAD",
        "OPTIONS"
    ))

    private val statusComboBox = JComboBox(arrayOf(
        StatusFilterItem("All Status", StatusFilter.ALL),
        StatusFilterItem("2xx Success", StatusFilter.SUCCESS),
        StatusFilterItem("3xx Redirect", StatusFilter.REDIRECT),
        StatusFilterItem("4xx Client Error", StatusFilter.CLIENT_ERROR),
        StatusFilterItem("5xx Server Error", StatusFilter.SERVER_ERROR),
        StatusFilterItem("Errors", StatusFilter.ERROR),
    ))

    private val deviceComboBox = JComboBox<DeviceFilterItem>().apply {
        addItem(DeviceFilterItem("All Devices", null, null))
    }

    init {
        border = JBUI.Borders.empty(0, 4)

        add(searchField)
        add(JBLabel(AllIcons.General.Filter))
        add(methodComboBox)
        add(statusComboBox)
        add(deviceComboBox)

        setupListeners()
        observeConnectedDevices()
    }

    private fun setupListeners() {
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updateFilter()
            }
        })

        methodComboBox.addActionListener { updateFilter() }
        statusComboBox.addActionListener { updateFilter() }
        deviceComboBox.addActionListener { updateFilter() }
    }

    private fun updateFilter() {
        val searchText = searchField.text.trim()
        val selectedMethod = methodComboBox.selectedItem as? String
        val methodFilter = if (selectedMethod == "All Methods") null else selectedMethod

        val statusItem = statusComboBox.selectedItem as? StatusFilterItem
        val statusFilter = statusItem?.filter ?: StatusFilter.ALL

        val deviceItem = deviceComboBox.selectedItem as? DeviceFilterItem
        val deviceFilter = deviceItem?.deviceId

        floconService.updateFilter(NetworkFilter(
            searchText = searchText,
            methodFilter = methodFilter,
            statusFilter = statusFilter,
            deviceFilter = deviceFilter,
        ))
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
                    item.packageName == currentSelection.packageName) {
                    deviceComboBox.selectedIndex = i
                    break
                }
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

/**
 * Combo box item for status filter.
 */
private data class StatusFilterItem(
    val displayName: String,
    val filter: StatusFilter
) {
    override fun toString(): String = displayName
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
