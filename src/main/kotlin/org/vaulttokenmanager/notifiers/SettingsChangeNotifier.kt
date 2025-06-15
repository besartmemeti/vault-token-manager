package org.vaulttokenmanager.notifiers

import com.intellij.util.messages.Topic

/**
 * Message bus interface for broadcasting vault configuration changes.
 * Ensures all active components can immediately react to settings updates
 * such as changes to executable path, server address, or timeout values.
 */
interface SettingsChangeNotifier {
    /**
     * Invoked when any vault configuration setting has been modified
     * through the settings panel or programmatically
     */
    fun settingsChanged()

    companion object {
        /**
         * Topic for the application message bus
         */
        @JvmStatic
        val TOPIC = Topic.create(
            "Settings Changed",
            SettingsChangeNotifier::class.java
        )
    }
}
