package org.vaulttokenmanager.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import org.vaulttokenmanager.notifiers.SettingsChangeNotifier
import java.io.File
import java.time.Duration

/**
 * Manages persistent storage for Vault plugin configuration including
 * server address, token validity periods, and executable locations.
 * Settings are saved to TokenSettings.xml in the IDE config directory.
 */
@Service
@State(
    name = "org.vaulttokenmanager.VaultConfigService",
    storages = [Storage("TokenSettings.xml")]
)
class ConfigService : PersistentStateComponent<ConfigService> {
    var vaultAddress: String = "https://vault.your-address.com"
    var tokenValidityHours: Long = 12
    var loginTimeoutSeconds: Long = 60
    var vaultExecutablePath: String = getDefaultVaultPath()

    /**
     * Converts configured hours to a Duration object for token expiration calculations
     */
    fun getTokenValidityDuration(): Duration = Duration.ofHours(tokenValidityHours)

    /**
     * Updates all configuration settings and broadcasts a change notification
     * to all registered listeners via the application message bus
     */
    fun updateSettings(
        vaultAddress: String,
        tokenValidityHours: Long,
        loginTimeoutSeconds: Long,
        vaultExecutablePath: String
    ) {
        this.vaultAddress = vaultAddress
        this.tokenValidityHours = tokenValidityHours
        this.loginTimeoutSeconds = loginTimeoutSeconds
        this.vaultExecutablePath = vaultExecutablePath

        notifySettingsChanged()
    }

    private fun notifySettingsChanged() {
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(SettingsChangeNotifier.TOPIC)
            .settingsChanged()
    }

    override fun getState(): ConfigService = this

    override fun loadState(state: ConfigService) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): ConfigService = service()

        /**
         * Determines the default vault CLI executable path based on the host operating system
         * - macOS: Homebrew installation location
         * - Windows: Standard Program Files location
         * - Linux/Unix: Standard binary path
         */
        @JvmStatic
        fun getDefaultVaultPath(): String {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("mac") -> "/opt/homebrew/bin/vault"
                osName.contains("win") -> "C:\\Program Files\\vault\\vault.exe"
                else -> "/usr/bin/vault" // Linux/Unix default
            }
        }

        /**
         * Validates that the vault executable exists at the given path and has execution permissions
         */
        @JvmStatic
        fun executableExists(path: String): Boolean {
            return File(path).exists() && File(path).canExecute()
        }
    }
}
