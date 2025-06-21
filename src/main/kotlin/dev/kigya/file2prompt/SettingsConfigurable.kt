package dev.kigya.file2prompt

import com.intellij.openapi.options.Configurable
import javax.swing.*
import com.intellij.ui.components.JBRadioButton

class SettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private lateinit var copyOnlyRb: JBRadioButton
    private lateinit var copyAndOpenRb: JBRadioButton

    override fun getDisplayName() = "File2Prompt"

    override fun createComponent(): JComponent {
        if (panel == null) {
            copyOnlyRb = JBRadioButton("Copy to clipboard only")
            copyAndOpenRb = JBRadioButton("Copy and open .txt file")

            ButtonGroup().apply {
                add(copyOnlyRb); add(copyAndOpenRb)
            }

            panel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                add(copyOnlyRb)
                add(Box.createVerticalStrut(5))
                add(copyAndOpenRb)
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean {
        val state = SettingsService.getInstance().state
        return (copyAndOpenRb.isSelected != state.openFileAfterCopy)
    }

    override fun apply() {
        val service = SettingsService.getInstance()
        service.state.openFileAfterCopy = copyAndOpenRb.isSelected
    }

    override fun reset() {
        val state = SettingsService.getInstance().state
        copyOnlyRb.isSelected = !state.openFileAfterCopy
        copyAndOpenRb.isSelected = state.openFileAfterCopy
    }

    override fun disposeUIResources() {
        panel = null
    }
}
