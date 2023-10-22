package com.github.pberdnik.dependencyhighlighter.fileui

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color

class ProjectPanelColorProvider : EditorTabColorProvider {
    override fun getEditorTabColor(project: Project, file: VirtualFile): Color? {
        return getColor(project, file)
    }

    override fun getProjectViewColor(project: Project, file: VirtualFile): Color? {
        return getColor(project, file)
    }

    private fun getColor(project: Project, file: VirtualFile): JBColor? {
        val service = project.service<ProjectViewUiStateService>()
        return service.fileColorMap[file.path]
    }
}