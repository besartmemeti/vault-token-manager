package org.vaulttokenmanager.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.PlainDocument

/**
 * Settings component for Vault Token Manager plugin.
 * Creates and manages the UI form displayed in the IntelliJ settings dialog.
 */
class VaultTokenSettingsComponent {
    // Form fields
    private val vaultAddressField = JBTextField()
    private val validityHoursField = JBTextField()
    private val loginTimeoutField = JBTextField()
    private val vaultExecutableField = TextFieldWithBrowseButton()

    // The main settings panel component
    private val _panel: JComponent

    init {
        // Restrict numeric fields to digits only
        validityHoursField.document = NumbersOnlyDocument()
        loginTimeoutField.document = NumbersOnlyDocument()

        // Setup file chooser for vault executable
        setupVaultExecutableField()

        // Build form using IntelliJ's FormBuilder for consistent layout
        _panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Vault Address:"), vaultAddressField, 1, true)
            .addLabeledComponent(JBLabel("Token Validity (hours):"), validityHoursField, 1, true)
            .addLabeledComponent(JBLabel("Login Timeout (seconds):"), loginTimeoutField, 1, true)
            .addLabeledComponent(JBLabel("Vault Executable Path:"), vaultExecutableField, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel
            .apply {
                border = JBUI.Borders.empty(10)
            }
    }

    /**
     * Configures the file chooser for selecting the vault executable
     */
    private fun setupVaultExecutableField() {
        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select Vault Executable")
            .withDescription("Choose the vault executable file")

        vaultExecutableField.addBrowseFolderListener(TextBrowseFolderListener(fileChooserDescriptor))
    }

    var vaultAddressValue: String
        get() = vaultAddressField.text
        set(value) {
            vaultAddressField.text = value
        }

    var tokenValidityHoursValue: String
        get() = validityHoursField.text
        set(value) {
            validityHoursField.text = value
        }

    var loginTimeoutValue: String
        get() = loginTimeoutField.text
        set(value) {
            loginTimeoutField.text = value
        }

    var vaultExecutablePath: String
        get() = vaultExecutableField.text
        set(value) {
            vaultExecutableField.text = value
        }

    // Public property to access the panel
    val panel: JComponent
        get() = _panel
}

/**
 * Custom document filter that restricts input to numeric digits only
 */
class NumbersOnlyDocument : PlainDocument() {
    @Throws(BadLocationException::class)
    override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
        if (str == null) {
            return
        }

        val newStr = str.replace(Regex("[^0-9]"), "")
        super.insertString(offs, newStr, a)
    }
}
