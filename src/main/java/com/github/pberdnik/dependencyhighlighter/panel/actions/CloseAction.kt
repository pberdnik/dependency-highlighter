package com.github.pberdnik.dependencyhighlighter.panel.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.FileDependenciesToolWindow
import com.intellij.CommonBundle
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.ui.content.Content

class CloseAction(
        private val myProject: Project,
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val myContent: Content?,
) : AnAction(CommonBundle.messagePointer("action.close"), CodeInsightBundle.messagePointer("action.close.dependency.description"),
        AllIcons.Actions.Cancel), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        myProject.getService(FileDependenciesToolWindow::class.java).closeContent(myContent)
        mySettings.copyToApplicationDependencySettings()
    }
}