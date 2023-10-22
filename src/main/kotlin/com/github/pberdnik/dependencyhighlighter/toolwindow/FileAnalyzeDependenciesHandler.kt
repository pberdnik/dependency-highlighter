package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.actions.ForwardDependenciesBuilder
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import javax.swing.SwingUtilities

class FileAnalyzeDependenciesHandler(
    private val project: Project,
    private val scopes: List<AnalysisScope>,
) {
    private val dependenciesHandler = project.service<DependenciesHandlerService>()

    fun analyze() {
        runBackgroundableTask(progressTitle, project, true) { indicator ->
            indicator.isIndeterminate = false
            perform()
            refreshPanel()
        }
    }

    private val progressTitle: String
        get() = CodeInsightBundle.message("package.dependencies.progress.title")

    private fun perform() {
        try {
            val builders = mutableListOf<DependenciesBuilder>()
            for (scope in scopes) {
                builders.add(ForwardDependenciesBuilder(project, scope))
            }
            for (builder in builders) {
                builder.analyze()
            }
            dependenciesHandler.updateDependencies(builders)
        } catch (e: IndexNotReadyException) {
            DumbService.getInstance(project).showDumbModeNotification(
                CodeInsightBundle.message("analyze.dependencies.not.available.notification.indexing")
            )
            throw ProcessCanceledException()
        }
    }

    private fun refreshPanel() {
        SwingUtilities.invokeLater {
            val displayName = "Dependencies"
            val panel = FileDependenciesPanel(project)
            val content = ContentFactory.getInstance().createContent(panel, displayName, false)
            content.setDisposer(panel)
            project.service<FileDependenciesToolWindow>().addContent(content)
        }
        ProjectView.getInstance(project).refresh()
    }
}