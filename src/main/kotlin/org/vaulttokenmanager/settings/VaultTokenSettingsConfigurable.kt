package org.vaulttokenmanager.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import org.vaulttokenmanager.services.ConfigService
import javax.swing.JComponent

/**
 * IntelliJ IDEA settings configurable for Vault Token Manager plugin.
 * Makes settings available in Preferences/Settings dialog (CMD + ,)
 */
class VaultTokenSettingsConfigurable : Configurable {
    private var settingsComponent: VaultTokenSettingsComponent? = null

    override fun getDisplayName(): String = "Vault Token Manager"

    override fun createComponent(): JComponent {
        settingsComponent = VaultTokenSettingsComponent()
        return settingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = ConfigService.getInstance()

        return settingsComponent?.let {
            it.vaultAddressValue != settings.vaultAddress ||
                    it.tokenValidityHoursValue.toLongOrNull() != settings.tokenValidityHours ||
                    it.loginTimeoutValue.toLongOrNull() != settings.loginTimeoutSeconds ||
                    it.vaultExecutablePath != settings.vaultExecutablePath
        } ?: false
    }

    /**
     * Validates settings before applying them.
     * Throws ConfigurationException with appropriate error message if validation fails.
     */
    @Throws(ConfigurationException::class)
    override fun apply() {
        val component = settingsComponent ?: return

        // Validate settings before applying
        val validationErrors = component.validateSettings()
        if (validationErrors.isNotEmpty()) {
            // Create an error message from all validation errors
            val errorMessage = buildString {
                append("Cannot apply settings due to validation errors:")
                validationErrors.values.forEach { error ->
                    append("\n• $error")
                }
            }
            throw ConfigurationException(errorMessage)
        }

        // All validations passed, apply settings
        val settings = ConfigService.getInstance()
        settings.updateSettings(
            component.vaultAddressValue,
            component.tokenValidityHoursValue.toLongOrNull() ?: 12L,
            component.loginTimeoutValue.toLongOrNull() ?: 60L,
            component.vaultExecutablePath
        )
    }

    override fun reset() {
        val settings = ConfigService.getInstance()
        settingsComponent?.let {
            it.vaultAddressValue = settings.vaultAddress
            it.tokenValidityHoursValue = settings.tokenValidityHours.toString()
            it.loginTimeoutValue = settings.loginTimeoutSeconds.toString()
            it.vaultExecutablePath = settings.vaultExecutablePath
        }
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
