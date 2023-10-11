package com.github.pberdnik.dependencyhighlighter.newplugin.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.pberdnik.dependencyhighlighter.MyBundle
import com.github.pberdnik.dependencyhighlighter.fileui.ProjectViewUiStateService
import javax.swing.JButton


class DependenciesToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dependenciesToolWindow = DependenciesToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(dependenciesToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class DependenciesToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<ProjectViewUiStateService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
