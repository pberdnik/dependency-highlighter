package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class FileDependenciesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        project.service<FileDependenciesToolWindow>().setContentManager(toolWindow.contentManager)
        val panel = FileDependenciesPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "Dependencies", false)
        toolWindow.contentManager.addContent(content)
    }
    override fun shouldBeAvailable(project: Project) = true
}