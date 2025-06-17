package org.vaulttokenmanager.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.io.File
import java.net.URL
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

    /**
     * Validates all settings fields and returns a map of field validation errors, if any.
     * @return Map of field names to error messages, empty if all fields are valid
     */
    fun validateSettings(): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Validate vault address (should be a valid URL or hostname)
        val address = vaultAddressValue.trim()
        if (address.isEmpty()) {
            errors["vaultAddress"] = "Vault address cannot be empty"
        } else {
            try {
                URL(address)
            } catch (e: Exception) {
                // If it's not a valid URL, check if it might be a hostname
                if (!address.matches(Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"))) {
                    errors["vaultAddress"] = "Invalid vault address format"
                }
            }
        }

        // Validate token validity hours (should be a positive number)
        val validityHours = tokenValidityHoursValue.trim()
        if (validityHours.isEmpty()) {
            errors["validityHours"] = "Token validity cannot be empty"
        } else {
            val hours = validityHours.toLongOrNull()
            if (hours == null || hours <= 0) {
                errors["validityHours"] = "Token validity must be a positive number"
            }
        }

        // Validate login timeout seconds (should be a positive number)
        val timeout = loginTimeoutValue.trim()
        if (timeout.isEmpty()) {
            errors["loginTimeout"] = "Login timeout cannot be empty"
        } else {
            val seconds = timeout.toLongOrNull()
            if (seconds == null || seconds <= 0) {
                errors["loginTimeout"] = "Login timeout must be a positive number"
            }
        }

        // Validate vault executable path (should be a valid executable file)
        val execPath = vaultExecutablePath.trim()
        if (execPath.isEmpty()) {
            errors["vaultExecutable"] = "Vault executable path cannot be empty"
        } else {
            val file = File(execPath)
            if (!file.exists()) {
                errors["vaultExecutable"] = "Vault executable file not found"
            } else if (!file.canExecute() || !file.isFile) {
                errors["vaultExecutable"] = "Vault executable does not have execution permissions"
            }
        }

        return errors
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
