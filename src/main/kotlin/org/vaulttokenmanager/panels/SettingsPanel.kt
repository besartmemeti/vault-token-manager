package org.vaulttokenmanager.panels

import com.intellij.ide.actions.ShowSettingsUtilImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import org.vaulttokenmanager.notifiers.SettingsChangeNotifier
import org.vaulttokenmanager.services.ConfigService
import org.vaulttokenmanager.settings.VaultTokenSettingsConfigurable
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * A simplified settings panel that displays current settings and provides
 * a link to the standard IntelliJ settings page for the plugin.
 */
class SettingsPanel : JPanel(), SettingsChangeNotifier {
    // Connection to the application's message bus
    private var connection = ApplicationManager.getApplication().messageBus.connect()

    // Labels to display current settings
    private val vaultAddressValueLabel = JBLabel()
    private val tokenValidityValueLabel = JBLabel()
    private val loginTimeoutValueLabel = JBLabel()
    private val vaultExecutableValueLabel = JBLabel()

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
        border = JBUI.Borders.empty(20)

        val gbc = GridBagConstraints()

        // Main message
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = JBUI.insetsBottom(15)
        val infoLabel = JBLabel("Plugin settings are accessible through IDE preferences")
        add(infoLabel, gbc)

        // Link to settings
        gbc.gridy = 1
        gbc.insets = JBUI.insetsBottom(20)
        val settingsLink = LinkLabel.create("Open Vault Token Manager Settings") {
            ShowSettingsUtilImpl.showSettingsDialog(
                null,
                VaultTokenSettingsConfigurable::class.java.name, ""
            )
        }
        add(settingsLink, gbc)

        // Current settings header
        gbc.gridy = 2
        gbc.insets = JBUI.insetsBottom(10)
        add(JBLabel("Current Settings:"), gbc)

        // Display current settings as read-only info
        gbc.gridwidth = 1
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5, 5, 5, 15)

        // Vault Address field
        gbc.gridx = 0
        gbc.gridy = 3
        add(JBLabel("Vault Address:"), gbc)
        gbc.gridx = 1
        add(vaultAddressValueLabel, gbc)

        // Token Validity field
        gbc.gridx = 0
        gbc.gridy = 4
        add(JBLabel("Token Validity (hours):"), gbc)
        gbc.gridx = 1
        add(tokenValidityValueLabel, gbc)

        // Login Timeout field
        gbc.gridx = 0
        gbc.gridy = 5
        add(JBLabel("Login Timeout (seconds):"), gbc)
        gbc.gridx = 1
        add(loginTimeoutValueLabel, gbc)

        // Vault Executable Path field
        gbc.gridx = 0
        gbc.gridy = 6
        add(JBLabel("Vault Executable Path:"), gbc)
        gbc.gridx = 1
        add(vaultExecutableValueLabel, gbc)
    }

    /**
     * Loads the current configuration values into display labels
     */
    private fun refreshFieldsFromConfig() {
        val config = ConfigService.getInstance()
        vaultAddressValueLabel.text = config.vaultAddress
        tokenValidityValueLabel.text = config.tokenValidityHours.toString()
        loginTimeoutValueLabel.text = config.loginTimeoutSeconds.toString()
        vaultExecutableValueLabel.text = config.vaultExecutablePath
    }

    /**
     * Handles notification of settings changes from other panels
     * by refreshing the UI with current configuration values
     */
    override fun settingsChanged() {
        SwingUtilities.invokeLater {
            refreshFieldsFromConfig()
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
