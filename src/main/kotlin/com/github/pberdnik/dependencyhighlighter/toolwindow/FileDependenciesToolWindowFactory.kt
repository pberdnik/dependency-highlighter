package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class FileDependenciesToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        FileDependenciesToolWindow.getInstance(project).initToolWindow(toolWindow)
    }
}