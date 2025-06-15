package org.vaulttokenmanager

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.vaulttokenmanager.panels.SettingsPanel
import org.vaulttokenmanager.panels.TokenPanel

class TokenToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val factory = ContentFactory.getInstance()

        // Create Overview tab
        val tokenPanel = TokenPanel()
        val tokenContent = factory.createContent(tokenPanel, "Overview", false)
        contentManager.addContent(tokenContent)

        // Create Settings tab
        val settingsPanel = SettingsPanel()
        val settingsContent = factory.createContent(settingsPanel, "Settings", false)
        contentManager.addContent(settingsContent)
    }
}
