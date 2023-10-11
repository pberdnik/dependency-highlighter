package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import javax.swing.SwingUtilities

private const val GREEN_MODULES = "Green Modules"

@Service(Service.Level.PROJECT)
class FileDependenciesToolWindow(private val project: Project) {
    private var contentManager: ContentManager? = null

    fun initToolWindow(toolWindow: ToolWindow) {
        project.service<StartupManager>().runAfterOpened {
            SwingUtilities.invokeLater {
                contentManager = toolWindow.contentManager
                toolWindow.setAvailable(true, null)
                val panel = ModulesPanel(project)
                val content = ContentFactory.getInstance().createContent(panel, GREEN_MODULES, false)
                addContent(content)
            }
        }
    }

    fun addContent(content: Content) {
        val contentManager = contentManager ?: return
        project.service<StartupManager>().runAfterOpened {
            SwingUtilities.invokeLater {
//            contentManager.removeAllContents(false)
                removeContentsExceptModules(contentManager)
                contentManager.addContent(content)
                contentManager.setSelectedContent(content)
//                ToolWindowManager.getInstance(project).getToolWindow("File Dependencies")!!.activate(null)
            }
        }
    }

    private fun removeContentsExceptModules(contentManager: ContentManager) {
        val contents = contentManager.contents.clone()
        contents.forEach { content ->
            if (!content.displayName.equals(GREEN_MODULES)) {
                contentManager.removeContent(content, true)
            }
        }
    }

    fun closeContent(content: Content?) {
        contentManager?.removeContent(content!!, true)
    }
}