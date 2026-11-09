package io.github.setheclark.intellij.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.UIUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.input.SAXBuilder
import java.io.StringReader
import javax.swing.JComponent

class NetworkHistoryConfigurableTest : BasePlatformTestCase() {

    private val state get() = NetworkStorageSettingsState.getInstance()
    private lateinit var configurable: NetworkHistoryConfigurable
    private lateinit var component: JComponent

    override fun setUp() {
        super.setUp()
        state.loadState(NetworkStorageSettingsState())
        configurable = NetworkHistoryConfigurable()
        component = configurable.createComponent()!!
        configurable.reset()
    }

    override fun tearDown() {
        try {
            configurable.disposeUIResources()
            state.loadState(NetworkStorageSettingsState())
        } finally {
            super.tearDown()
        }
    }

    fun testPageShowsTheThreeHistoryOptionsWithTheirDefaultsAndNothingElse() {
        val persist = UIUtil.findComponentsOfType(component, JBCheckBox::class.java).single()
        val spinners = UIUtil.findComponentsOfType(component, JBIntSpinner::class.java)

        assertTrue(persist.isSelected)
        assertEquals(listOf(1024, 7), spinners.map { it.number })
    }

    fun testApplyingThePageWritesTheChosenOptionsToTheSavedSettings() {
        val persist = UIUtil.findComponentsOfType(component, JBCheckBox::class.java).single()
        val (size, age) = UIUtil.findComponentsOfType(component, JBIntSpinner::class.java)
        assertFalse(configurable.isModified)

        persist.isSelected = false
        size.number = 256
        age.number = 3
        assertTrue(configurable.isModified)
        configurable.apply()

        assertFalse(state.persistHistory)
        assertEquals(256, state.maxDatabaseSizeMb)
        assertEquals(3, state.maxAgeDays)
        assertFalse(configurable.isModified)
    }

    fun testResettingThePageRestoresTheSavedSettings() {
        val persist = UIUtil.findComponentsOfType(component, JBCheckBox::class.java).single()
        persist.isSelected = false
        assertTrue(configurable.isModified)

        configurable.reset()

        assertTrue(persist.isSelected)
        assertTrue(state.persistHistory)
    }

    fun testSettingsSavedBeforeTheHistoryOptionsLoadAndKeepTheirLayoutSettings() {
        val saved = """
            <component name="FloconNetworkStorageSettings">
              <option name="callDetailOpenMode" value="EDITOR_WINDOW" />
              <option name="columnWidths"><map><entry key="URL" value="355.0" /></map></option>
              <option name="detailLeftPanelWidthDp" value="383.0" />
              <option name="hiddenColumns"><set><option value="SIZE" /></set></option>
              <option name="maxStoredCalls" value="500" />
              <option name="maxBodyCacheSizeMb" value="64" />
              <option name="maxBodySizeKb" value="256" />
              <option name="compressBodies" value="false" />
            </component>
        """.trimIndent()

        val loaded = XmlSerializer.deserialize(
            SAXBuilder().build(StringReader(saved)).rootElement,
            NetworkStorageSettingsState::class.java,
        )
        state.loadState(loaded)

        assertEquals(setOf("SIZE"), state.hiddenColumns)
        assertEquals(mapOf("URL" to 355.0f), state.columnWidths)
        assertEquals(383.0f, state.detailLeftPanelWidthDp)
        assertEquals(CallDetailOpenMode.EDITOR_WINDOW, state.callDetailOpenMode)
        assertTrue(state.persistHistory)
        assertEquals(1024, state.maxDatabaseSizeMb)
        assertEquals(7, state.maxAgeDays)
    }
}
