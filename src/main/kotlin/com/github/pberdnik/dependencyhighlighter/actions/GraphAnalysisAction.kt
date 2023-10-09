package com.github.pberdnik.dependencyhighlighter.actions

import com.github.pberdnik.dependencyhighlighter.storage.GraphStorageService
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import icons.SdkIcons

class GraphAnalysisAction : AnAction("Run Graph Analysis", "Run graph analysis", SdkIcons.dependencies) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<GraphStorageService>().analyze()
        ProjectView.getInstance(project).refresh()
    }
}