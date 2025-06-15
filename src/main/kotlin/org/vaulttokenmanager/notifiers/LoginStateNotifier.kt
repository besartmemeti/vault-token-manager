package org.vaulttokenmanager.notifiers

import com.intellij.util.messages.Topic

/**
 * Message bus interface for synchronizing vault login state across multiple tool windows.
 * Allows components to react to login process status changes regardless of which
 * component initiated the login.
 */
interface LoginStateNotifier {
    /**
     * Called when vault login process starts or completes
     *
     * @param isLoginInProgress True during active login process, false when completed or canceled
     */
    fun loginStateChanged(isLoginInProgress: Boolean)

    companion object {
        /**
         * Topic for the application message bus
         */
        @JvmStatic
        val TOPIC = Topic.create(
            "Login State Changed",
            LoginStateNotifier::class.java
        )
    }
}
