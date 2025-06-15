package org.vaulttokenmanager.panels

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.vaulttokenmanager.exceptions.LoginException
import org.vaulttokenmanager.notifiers.LoginStateNotifier
import org.vaulttokenmanager.services.NotificationService
import org.vaulttokenmanager.notifiers.SettingsChangeNotifier
import org.vaulttokenmanager.services.TokenService
import java.awt.Color
import java.awt.Cursor
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.time.Duration
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Main token status panel that displays vault token state and provides
 * authentication controls. Updates in real-time to reflect token validity,
 * login state, and executable availability.
 */
class TokenPanel : JPanel(), SettingsChangeNotifier, LoginStateNotifier {
    private val greenColor = JBColor("Green", Color(0, 128, 0))
    private val redColor = JBColor("Red", Color(255, 0, 0))
    private val orangeColor = JBColor("Orange", Color(255, 165, 0))

    private val statusLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
    }
    private val validityLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
    }
    private val loadingLabel = JBLabel("Login process ongoing...").apply {
        horizontalAlignment = SwingConstants.CENTER
        foreground = orangeColor
        isVisible = false
    }
    private val errorLabel = JBLabel().apply {
        horizontalAlignment = SwingConstants.CENTER
        foreground = redColor
        isVisible = false
    }
    private val generateButton = JButton("Generate Token").apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }
    private val cancelButton = JButton("Cancel Login").apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        isVisible = false
    }

    // Timer that refreshes token status every second to show accurate remaining validity
    private val updateTimer = Timer(1000) { updateTokenStatus() }

    private var connection = ApplicationManager.getApplication().messageBus.connect()

    init {
        connection.subscribe(SettingsChangeNotifier.TOPIC, this)
        connection.subscribe(LoginStateNotifier.TOPIC, this)

        setupUI()

        // Initialize panel with current state
        updateLoginInProgressUI(TokenService.getInstance().isLoginInProgress())
        updateTokenStatus()
        updateTimer.start()
    }

    private fun setupUI() {
        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        val centerPanel = JPanel(GridBagLayout())

        // Status label shows if token is valid/invalid
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(5, 0)
        centerPanel.add(statusLabel, gbc)

        // Validity label shows remaining time for valid tokens
        gbc.gridy = 1
        gbc.insets = JBUI.insetsBottom(10)
        centerPanel.add(validityLabel, gbc)

        // Error label for executable not found and other issues
        gbc.gridy = 2
        gbc.insets = JBUI.insetsBottom(10)
        centerPanel.add(errorLabel, gbc)

        // Loading indicator during OIDC authentication
        gbc.gridy = 3
        gbc.insets = JBUI.insetsBottom(10)
        centerPanel.add(loadingLabel, gbc)

        // Token generation button
        gbc.gridy = 4
        gbc.insets = JBUI.insetsBottom(5)
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        centerPanel.add(generateButton, gbc)

        // Cancel button for interrupting login
        gbc.gridy = 5
        gbc.insets = JBUI.insetsBottom(5)
        centerPanel.add(cancelButton, gbc)

        // Reset constraints for main layout
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 1.0

        // Add components to main panel with vertical centering
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.NONE

        add(Box.createVerticalGlue(), gbc)
        gbc.gridy = 1
        add(centerPanel, gbc)
        gbc.gridy = 2
        add(Box.createVerticalGlue(), gbc)

        generateButton.addActionListener { generateToken() }
        cancelButton.addActionListener { cancelLogin() }
    }

    /**
     * Refreshes token status UI based on current validation state and vault availability.
     * Called periodically by timer to continuously update remaining validity time.
     */
    private fun updateTokenStatus() {
        val tokenService = TokenService.getInstance()

        if (!tokenService.isVaultExecutableAvailable()) {
            updateExecutableNotFoundUI(tokenService.getVaultExecutablePath())
            return
        }

        errorLabel.isVisible = false

        if (tokenService.isVaultTokenValid()) {
            updateValidTokenUI(tokenService.getRemainingTokenValidity())
        } else {
            updateInvalidTokenUI(tokenService.isLoginInProgress())
        }
    }

    /**
     * Updates UI when vault CLI cannot be found at the configured location
     */
    private fun updateExecutableNotFoundUI(execPath: String) {
        statusLabel.text = "Vault executable not found"
        statusLabel.foreground = redColor
        validityLabel.isVisible = false
        errorLabel.text = "Please configure vault executable in settings: $execPath"
        errorLabel.isVisible = true
        generateButton.isEnabled = false
    }

    /**
     * Updates UI when token exists and is within validity period
     */
    private fun updateValidTokenUI(remainingValidity: Duration) {
        statusLabel.text = "Token is valid"
        statusLabel.foreground = greenColor
        validityLabel.text = "Valid for: ${formatDuration(remainingValidity)}"
        validityLabel.isVisible = true
        generateButton.isEnabled = false
    }

    /**
     * Updates UI when token is expired or missing
     */
    private fun updateInvalidTokenUI(isLoginInProgress: Boolean) {
        statusLabel.text = "Token is invalid or not found"
        statusLabel.foreground = redColor
        validityLabel.text = ""
        validityLabel.isVisible = false
        generateButton.isEnabled = !isLoginInProgress
    }

    /**
     * Terminates any ongoing OIDC login process
     */
    private fun cancelLogin() {
        TokenService.getInstance().cancelLoginProcess()
    }

    /**
     * Initiates token generation via OIDC authentication in a background thread
     * to prevent UI freezing during browser-based login flow
     */
    private fun generateToken() {
        val tokenService = TokenService.getInstance()

        if (!tokenService.isVaultExecutableAvailable()) {
            updateExecutableNotFoundUI(tokenService.getVaultExecutablePath())
            return
        }

        if (tokenService.isLoginInProgress()) {
            showNotification("Token generation already in progress", NotificationType.WARNING)
            return
        }

        Thread {
            try {
                tokenService.generateToken()
                SwingUtilities.invokeLater {
                    showNotification("Vault token generated successfully", NotificationType.INFORMATION)
                    updateTokenStatus()
                }
            } catch (e: LoginException) {
                handleLoginException(e)
            }
        }.start()
    }

    /**
     * Provides specialized error handling based on exception codes from the vault CLI
     */
    private fun handleLoginException(e: LoginException) {
        SwingUtilities.invokeLater {
            val notificationType: NotificationType
            val message: String

            when {
                e.code == 137 -> {
                    message = "Vault login process was canceled"
                    notificationType = NotificationType.WARNING
                }
                else -> {
                    message = e.message ?: "Unknown login error"
                    notificationType = NotificationType.ERROR
                }
            }

            showNotification(message, notificationType)
            updateTokenStatus()
        }
    }

    /**
     * Handles login state change notifications from the system message bus
     * enabling UI synchronization across multiple tool windows
     */
    override fun loginStateChanged(isLoginInProgress: Boolean) {
        SwingUtilities.invokeLater {
            updateLoginInProgressUI(isLoginInProgress)
        }
    }

    /**
     * Toggles visibility of UI components based on login state
     */
    private fun updateLoginInProgressUI(inProgress: Boolean) {
        loadingLabel.isVisible = inProgress
        generateButton.isVisible = !inProgress
        cancelButton.isVisible = inProgress
        validityLabel.isVisible = !inProgress
        statusLabel.isVisible = !inProgress
        errorLabel.isVisible = false

        updateTokenStatus()
    }

    private fun showNotification(content: String, type: NotificationType) {
        NotificationService.getInstance().showNotification(content, type)
    }

    /**
     * Formats a duration as HH:MM:SS with leading zeros
     */
    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    /**
     * Refreshes token status when vault configuration changes
     * that may affect token validation
     */
    override fun settingsChanged() {
        SwingUtilities.invokeLater { updateTokenStatus() }
    }

    override fun addNotify() {
        super.addNotify()
        updateTokenStatus()
        updateTimer.start()

        connection = ApplicationManager.getApplication().messageBus.connect()
        connection.subscribe(SettingsChangeNotifier.TOPIC, this)
        connection.subscribe(LoginStateNotifier.TOPIC, this)
    }

    override fun removeNotify() {
        super.removeNotify()
        updateTimer.stop()
        connection.disconnect()
    }
}
