package org.vaulttokenmanager.panels

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.vaulttokenmanager.services.ConfigService
import org.vaulttokenmanager.services.NotificationService
import org.vaulttokenmanager.notifiers.SettingsChangeNotifier
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.PlainDocument

/**
 * UI panel for configuring vault connection settings and token behavior.
 * Handles validation and persistence of server URL, token validity periods,
 * and executable path across IDE sessions.
 */
class SettingsPanel : JPanel(), SettingsChangeNotifier {
    private val vaultAddressField = JBTextField()
    private val validityHoursField = JBTextField()
    private val loginTimeoutField = JBTextField()
    private val vaultExecutableField = TextFieldWithBrowseButton()
    private val saveButton = JButton("Save Settings").apply {
        isEnabled = false
    }

    // Connection to the application's message bus
    private var connection = ApplicationManager.getApplication().messageBus.connect()

    init {
        // Subscribe to settings change notifications
        connection.subscribe(SettingsChangeNotifier.TOPIC, this)

        setupUI()

        // Load initial values
        refreshFieldsFromConfig()
    }

    /**
     * Sets up the UI components and layout
     */
    private fun setupUI() {
        layout = GridBagLayout()

        // Create form panel
        val formPanel = createFormPanel()

        // Add the form panel to the main panel
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            weighty = 1.0
            anchor = GridBagConstraints.NORTH
            fill = GridBagConstraints.HORIZONTAL
        }
        add(formPanel, gbc)

        // Binding actions
        saveButton.addActionListener { saveSettings() }

        // Add document listeners to enable save button when changes are made
        addDocumentListeners()
    }

    /**
     * Creates the settings form with input validation constraints:
     * - Numeric fields restricted to positive integers
     * - File chooser with executable filter for vault binary
     */
    private fun createFormPanel(): JPanel {
        val formPanel = JPanel(GridBagLayout())
        formPanel.border = JBUI.Borders.empty(20)
        val gbc = GridBagConstraints()

        // Restrict numeric fields to digits only
        validityHoursField.document = NumbersOnlyDocument(validityHoursField.text)
        loginTimeoutField.document = NumbersOnlyDocument(loginTimeoutField.text)

        // Setup file chooser for vault executable
        setupVaultExecutableField()

        // Vault Address field
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 5, 5, 15)
        formPanel.add(JBLabel("Vault Address:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(5, 0, 5, 5)
        vaultAddressField.columns = 30
        formPanel.add(vaultAddressField, gbc)

        // Token Validity field
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.insets = JBUI.insets(5, 5, 5, 15)
        formPanel.add(JBLabel("Token Validity (hours):"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(5, 0, 5, 5)
        validityHoursField.columns = 10
        formPanel.add(validityHoursField, gbc)

        // Login Timeout field
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.insets = JBUI.insets(5, 5, 5, 15)
        formPanel.add(JBLabel("Login Timeout (seconds):"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(5, 0, 5, 5)
        loginTimeoutField.columns = 10
        formPanel.add(loginTimeoutField, gbc)

        // Vault Executable Path field
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.insets = JBUI.insets(5, 5, 5, 15)
        formPanel.add(JBLabel("Vault Executable Path:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(5, 0, 5, 5)
        formPanel.add(vaultExecutableField, gbc)

        // Save button
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.weightx = 0.0
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = JBUI.insets(15, 5, 5, 5)
        formPanel.add(saveButton, gbc)

        return formPanel
    }

    /**
     * Configures the file chooser for selecting the vault executable
     * with appropriate file filters and UI feedback
     */
    private fun setupVaultExecutableField() {
        val fileChooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select Vault Executable")
            .withDescription("Choose the vault executable file")

        vaultExecutableField.addBrowseFolderListener(TextBrowseFolderListener(fileChooserDescriptor))
        vaultExecutableField.textField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { checkForChanges() }
            override fun removeUpdate(e: DocumentEvent) { checkForChanges() }
            override fun changedUpdate(e: DocumentEvent) { checkForChanges() }
        })
    }

    /**
     * Loads the current configuration values into form fields
     */
    private fun refreshFieldsFromConfig() {
        val config = ConfigService.getInstance()
        vaultAddressField.text = config.vaultAddress
        validityHoursField.text = config.tokenValidityHours.toString()
        loginTimeoutField.text = config.loginTimeoutSeconds.toString()
        vaultExecutableField.text = config.vaultExecutablePath
    }

    /**
     * Sets up change detection on form fields to enable/disable save button
     */
    private fun addDocumentListeners() {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { checkForChanges() }
            override fun removeUpdate(e: DocumentEvent) { checkForChanges() }
            override fun changedUpdate(e: DocumentEvent) { checkForChanges() }
        }

        vaultAddressField.document.addDocumentListener(listener)
        validityHoursField.document.addDocumentListener(listener)
        loginTimeoutField.document.addDocumentListener(listener)
    }

    /**
     * Compares current field values with stored configuration
     * to determine if changes need to be saved
     */
    private fun checkForChanges() {
        val config = ConfigService.getInstance()
        val addressChanged = vaultAddressField.text != config.vaultAddress

        val validityHoursText = validityHoursField.text
        val validityHoursChanged = if (validityHoursText.isNotEmpty()) {
            validityHoursText.toLongOrNull() != config.tokenValidityHours
        } else {
            true
        }

        val timeoutText = loginTimeoutField.text
        val timeoutChanged = if (timeoutText.isNotEmpty()) {
            timeoutText.toLongOrNull() != config.loginTimeoutSeconds
        } else {
            true
        }

        val executablePathChanged = vaultExecutableField.text != config.vaultExecutablePath

        saveButton.isEnabled = addressChanged || validityHoursChanged || timeoutChanged || executablePathChanged
    }

    /**
     * Validates and persists the current form values to configuration storage
     */
    private fun saveSettings() {
        try {
            if (!validateSettings()) {
                return
            }

            // Get current values
            val vaultAddress = vaultAddressField.text
            val validityHours = validityHoursField.text.toLongOrNull() ?: 12L
            val loginTimeout = loginTimeoutField.text.toLongOrNull() ?: 60L
            val vaultExecutablePath = vaultExecutableField.text

            // Save to configuration service
            ConfigService.getInstance().updateSettings(
                vaultAddress,
                validityHours,
                loginTimeout,
                vaultExecutablePath
            )

            // Disable save button as changes are now saved
            saveButton.isEnabled = false

            showNotification("Settings saved successfully", NotificationType.INFORMATION)
        } catch (e: Exception) {
            showNotification("Failed to save settings: ${e.message}", NotificationType.ERROR)
        }
    }

    /**
     * Performs validation checks on the form values:
     * - Positive values for numeric fields
     * - Executable existence and permissions for vault binary path
     */
    private fun validateSettings(): Boolean {
        val validityHours = validityHoursField.text.toLongOrNull() ?: 0L
        if (validityHours <= 0) {
            showNotification("Token validity must be a positive number of hours", NotificationType.ERROR)
            return false
        }

        val loginTimeout = loginTimeoutField.text.toLongOrNull() ?: 0L
        if (loginTimeout <= 0) {
            showNotification("Login timeout must be a positive number of seconds", NotificationType.ERROR)
            return false
        }

        val vaultExecutablePath = vaultExecutableField.text
        if (vaultExecutablePath.isNotEmpty() && !ConfigService.executableExists(vaultExecutablePath)) {
            showNotification(
                "The specified vault executable does not exist or is not executable",
                NotificationType.ERROR
            )
            return false
        }

        return true
    }

    /**
     * Shows an IntelliJ notification
     */
    private fun showNotification(content: String, type: NotificationType) {
        NotificationService.getInstance().showNotification(content, type)
    }

    /**
     * Handles notification of settings changes from other panels
     * by refreshing the UI with current configuration values
     */
    override fun settingsChanged() {
        SwingUtilities.invokeLater {
            refreshFieldsFromConfig()
            saveButton.isEnabled = false
        }
    }

    override fun addNotify() {
        super.addNotify()
        connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(SettingsChangeNotifier.TOPIC, this)
        refreshFieldsFromConfig()
    }

    override fun removeNotify() {
        super.removeNotify()
        connection.disconnect()
    }
}

/**
 * Custom document filter that restricts input to numeric digits only,
 * used for token validity and timeout fields
 */
class NumbersOnlyDocument(initialValue: String = "") : PlainDocument() {
    init {
        if (initialValue.isNotEmpty()) {
            insertString(0, initialValue, null)
        }
    }

    @Throws(BadLocationException::class)
    override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
        if (str == null) {
            return
        }

        val newStr = str.replace(Regex("[^0-9]"), "")
        super.insertString(offs, newStr, a)
    }
}
