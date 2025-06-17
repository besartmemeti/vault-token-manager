package org.vaulttokenmanager.settings

import com.intellij.openapi.options.Configurable
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

    override fun apply() {
        val settings = ConfigService.getInstance()
        settingsComponent?.let {
            settings.updateSettings(
                it.vaultAddressValue,
                it.tokenValidityHoursValue.toLongOrNull() ?: 12L,
                it.loginTimeoutValue.toLongOrNull() ?: 60L,
                it.vaultExecutablePath
            )
        }
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
