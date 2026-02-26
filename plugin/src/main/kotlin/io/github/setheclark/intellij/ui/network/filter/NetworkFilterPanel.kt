package io.github.setheclark.intellij.ui.network.filter

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.ViewModelCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

@Inject
class NetworkFilterPanel(
    @param:ViewModelCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkFilterViewModel,
) : JPanel() {

    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = "Filter requests..."
    }

    private val deviceComboBox = JComboBox<DeviceFilterItem>()
    private val deviceSelectionListener = { _: ActionEvent -> dispatchDeviceUpdate() }

    init {
        // Setup filter listeners
        setupFilterListeners()

        layout = BorderLayout(8, 0)
        border = JBUI.Borders.empty(4, 8)
        add(deviceComboBox, BorderLayout.WEST)
        add(searchField, BorderLayout.CENTER)

        observeState()
    }

    private fun observeState() {
        scope.launch {
            viewModel.state.map { it.devices }.distinctUntilChanged().collectLatest {
                updateDeviceComboBox(it)
            }
        }
    }

    private fun setupFilterListeners() {
        searchField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                dispatchFilterUpdate()
            }
        })

        deviceComboBox.addActionListener(deviceSelectionListener)
    }

    private fun dispatchFilterUpdate() {
        val searchText = searchField.text.trim()

        viewModel.dispatch(
            NetworkFilterIntent.UpdateFilter(searchText),
        )
    }

    private fun dispatchDeviceUpdate() {
        (deviceComboBox.selectedItem as? DeviceFilterItem)?.let {
            viewModel.dispatch(
                NetworkFilterIntent.UpdateDeviceSelection(it),
            )
        }
    }

    private fun updateDeviceComboBox(renderModel: DevicesRenderModel) {
        deviceComboBox.removeActionListener(deviceSelectionListener)
        try {
            deviceComboBox.removeAllItems()
            renderModel.devices.forEach { deviceComboBox.addItem(it) }
            deviceComboBox.selectedIndex = renderModel.selectedIndex
        } finally {
            deviceComboBox.addActionListener(deviceSelectionListener)
        }
    }
}
