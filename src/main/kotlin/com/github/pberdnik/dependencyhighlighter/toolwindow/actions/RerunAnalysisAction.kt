package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.DependenciesHandlerService
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import javax.swing.SwingUtilities

class RerunAnalysisAction(
    private val project: Project,
    private val rebuild: () -> Unit,
) : AnAction(
    "Rerun Analysis", "Rerun analysis on changed files",
    AllIcons.Actions.Rerun
) {

    private val dependenciesHandler = project.service<DependenciesHandlerService>()

    override fun actionPerformed(e: AnActionEvent) {
        val progressTitle = CodeInsightBundle.message("package.dependencies.progress.title")
        runBackgroundableTask(progressTitle, project, true) { indicator ->
            indicator.isIndeterminate = false
            dependenciesHandler.reanalyseChanged()
            SwingUtilities.invokeLater {
                rebuild()
            }
        }
    }
}
