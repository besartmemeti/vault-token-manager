package org.vaulttokenmanager.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import org.vaulttokenmanager.exceptions.LoginException
import org.vaulttokenmanager.notifiers.LoginStateNotifier
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Handles Vault token lifecycle operations including OIDC-based authentication,
 * token validation, and expiration management across multiple IDE windows.
 */
@Service
class TokenService {
    private val tokenFilePath = System.getProperty("user.home") + "/.vault-token"
    private val logger: Logger = Logger.getInstance(TokenService::class.java)

    private val currentProcess = AtomicReference<Process?>()
    private val isLoginInProgressFlag = AtomicBoolean(false)

    /**
     * Verifies that the configured vault CLI executable exists and has execution permissions
     */
    fun isVaultExecutableAvailable(): Boolean {
        val executablePath = ConfigService.getInstance().vaultExecutablePath
        return ConfigService.executableExists(executablePath)
    }

    fun getVaultExecutablePath(): String {
        return ConfigService.getInstance().vaultExecutablePath
    }

    /**
     * Calculates the remaining time until token expiration based on file timestamp
     * and configured validity period
     */
    fun getRemainingTokenValidity(): Duration {
        val file = File(tokenFilePath)
        if (file.exists()) {
            val lastModified = Instant.ofEpochMilli(file.lastModified())
            val age = Duration.between(lastModified, Instant.now())
            return ConfigService.getInstance().getTokenValidityDuration().minus(age)
        }
        return Duration.ZERO
    }

    /**
     * Determines if the current token is still valid based on its age
     */
    fun isVaultTokenValid(): Boolean {
        val remainingValidity = getRemainingTokenValidity()
        return !(remainingValidity.isNegative || remainingValidity.isZero)
    }

    fun isLoginInProgress(): Boolean {
        return isLoginInProgressFlag.get()
    }

    /**
     * Updates login state and broadcasts changes to all listeners
     * when the state actually changes
     */
    private fun setLoginInProgress(inProgress: Boolean) {
        val oldValue = isLoginInProgressFlag.getAndSet(inProgress)

        if (oldValue != inProgress) {
            notifyLoginStateChanged(inProgress)
        }
    }

    private fun notifyLoginStateChanged(isLoginInProgress: Boolean) {
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(LoginStateNotifier.TOPIC)
            .loginStateChanged(isLoginInProgress)
    }

    /**
     * Terminates any currently running vault login process and
     * resets the login state
     */
    fun cancelLoginProcess() {
        val process = currentProcess.get()
        if (process != null && process.isAlive) {
            process.destroyForcibly()
            currentProcess.set(null)
            setLoginInProgress(false)
            logger.info("Vault login process was manually canceled")
        }
    }

    /**
     * Initiates the vault OIDC login workflow to generate a new token
     *
     * @throws LoginException if authentication fails, times out, or
     *         the vault executable can't be found
     */
    @Throws(LoginException::class)
    fun generateToken() {
        if (isLoginInProgressFlag.get()) {
            throw LoginException("Login process already in progress")
        }

        setLoginInProgress(true)

        val config = ConfigService.getInstance()
        val vaultAddr = config.vaultAddress
        val vaultExecutablePath = config.vaultExecutablePath

        logger.debug("Setting VAULT_ADDR to: $vaultAddr")
        logger.debug("Using vault executable: $vaultExecutablePath")

        try {
            // Skip login if token is already valid
            if (isVaultTokenValid()) {
                setLoginInProgress(false)
                return
            }

            if (!ConfigService.executableExists(vaultExecutablePath)) {
                setLoginInProgress(false)
                throw LoginException("Vault executable not found at: $vaultExecutablePath")
            }

            executeLoginProcess(vaultExecutablePath, vaultAddr, config.loginTimeoutSeconds)
        } catch (e: IOException) {
            logger.error("IOException while running vault login command: $e")
            setLoginInProgress(false)
            throw LoginException("Failed to run vault login: ${e.message}")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("InterruptedException while running vault login command: $e")
            setLoginInProgress(false)
            throw LoginException("Vault login process was interrupted")
        }
    }

    /**
     * Launches the vault CLI process with OIDC authentication method and monitors completion
     *
     * @param vaultExecutablePath Path to the vault executable
     * @param vaultAddr Vault server address (VAULT_ADDR environment variable)
     * @param timeoutSeconds Maximum seconds to wait for login to complete
     * @throws LoginException if the process times out or returns a non-zero exit code
     */
    @Throws(IOException::class, InterruptedException::class, LoginException::class)
    private fun executeLoginProcess(
        vaultExecutablePath: String,
        vaultAddr: String,
        timeoutSeconds: Long
    ) {
        val processBuilder = ProcessBuilder(vaultExecutablePath, "login", "-method", "oidc")
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        processBuilder.environment().put("VAULT_ADDR", vaultAddr)
        processBuilder.directory(File(System.getProperty("user.dir")))

        logger.debug("Executing vault login command...")

        val process = processBuilder.start()
        currentProcess.set(process)

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        currentProcess.set(null)

        if (!completed) {
            process.destroyForcibly()
            setLoginInProgress(false)
            throw LoginException("Login process timed out after $timeoutSeconds seconds")
        }

        val exitCode = process.exitValue()

        if (exitCode != 0) {
            setLoginInProgress(false)
            throw LoginException("Vault login failed with exit code: $exitCode", exitCode)
        } else {
            logger.info("vault login command executed successfully.")
            setLoginInProgress(false)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): TokenService = service()
    }
}
