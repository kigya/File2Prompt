// src/test/kotlin/dev/kigya/file2prompt/SettingsServiceAndConfigurableTest.kt
package com.github.kigya.file2prompt

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.components.JBRadioButton
import dev.kigya.file2prompt.SettingsConfigurable
import dev.kigya.file2prompt.SettingsService
import org.junit.Test
import javax.swing.JPanel
import javax.swing.JRadioButton

class SettingsServiceAndConfigurableTest : LightPlatformTestCase() {

    @Test
    fun `test default settings state`() {
        val service = SettingsService.getInstance()
        // By default, openFileAfterCopy should be false
        assertFalse(service.state.openFileAfterCopy)
    }

    @Test
    fun `test modify settings state persists`() {
        val service = SettingsService.getInstance()
        service.state.openFileAfterCopy = true
        // After modification, state should reflect change
        assertTrue(service.state.openFileAfterCopy)

        // Reset back
        service.state.openFileAfterCopy = false
        assertFalse(service.state.openFileAfterCopy)
    }

    @Test
    fun `test settings configurable reset and apply`() {
        val service = SettingsService.getInstance()
        // ensure starting state
        service.state.openFileAfterCopy = false

        val configurable = SettingsConfigurable()
        val panel = configurable.createComponent() as JPanel

        // locate radio buttons by text
        val radioButtons = panel
            .components
            .filterIsInstance<JRadioButton>()
            .associateBy { it.text }

        val copyOnly = radioButtons["Copy to clipboard only"] ?: error("CopyOnly radio missing")
        val copyAndOpen = radioButtons["Copy and open .txt file"] ?: error("CopyAndOpen radio missing")

        // reset should reflect state=false => copyOnly selected
        configurable.reset()
        assertTrue(copyOnly.isSelected)
        assertFalse(copyAndOpen.isSelected)

        // simulate user selecting "copy and open"
        copyAndOpen.isSelected = true
        assertTrue(configurable.isModified())

        // apply should persist
        configurable.apply()
        assertTrue(service.state.openFileAfterCopy)

        // now reset will sync UI back to state=true
        configurable.reset()
        assertTrue(copyAndOpen.isSelected)
        assertFalse(copyOnly.isSelected)
    }
}
