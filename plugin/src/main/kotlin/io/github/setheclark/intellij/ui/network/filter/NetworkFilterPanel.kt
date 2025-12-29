package io.github.setheclark.intellij.ui.network.filter

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import dev.zacsweers.metro.Inject
import io.github.setheclark.intellij.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.awt.FlowLayout
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

@Inject
class NetworkFilterPanel(
    @param:AppCoroutineScope private val scope: CoroutineScope,
    private val viewModel: NetworkFilterViewModel,
) : JPanel() {

    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = "Filter requests..."
        preferredSize = JBUI.size(200, 28)
    }

    private val deviceComboBox = JComboBox<DeviceFilterItem>()

    init {
        // Setup filter listeners
        setupFilterListeners()

        layout = FlowLayout(FlowLayout.LEADING, 8, 0).apply {
            border = JBUI.Borders.empty()
            add(searchField)
            add(deviceComboBox)
        }

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

        deviceComboBox.addActionListener { dispatchDeviceUpdate() }
    }

    private fun dispatchFilterUpdate() {
        val searchText = searchField.text.trim()

        viewModel.dispatch(
            NetworkFilterIntent.UpdateFilter(searchText)
        )
    }

    private fun dispatchDeviceUpdate() {
        (deviceComboBox.selectedItem as? DeviceFilterItem)?.let {
            viewModel.dispatch(
                NetworkFilterIntent.UpdateDeviceSelection(it)
            )
        }
    }

    private fun updateDeviceComboBox(renderModel: DevicesRenderModel) {
        deviceComboBox.apply {
            removeAll()
            renderModel.devices.forEach { addItem(it) }
        }
        deviceComboBox.selectedIndex = renderModel.selectedIndex
    }
}