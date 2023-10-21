package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.actions.MyForwardDependenciesBuilder
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.content.ContentFactory
import javax.swing.SwingUtilities

class FileAnalyzeDependenciesHandler(
        private val project: Project,
        private val scopes: List<AnalysisScope>,
        private val myTransitiveBorder: Int,
        private val excluded: MutableSet<PsiFile> = HashSet()
) {
    fun analyze() {
        val builders = mutableListOf<MyDependenciesBuilder>()
        runBackgroundableTask(progressTitle, project, true) { indicator ->
            indicator.isIndeterminate = false
            perform(builders, indicator)
            refreshPanel(builders)
        }
    }

    private val progressTitle: String
        get() = CodeInsightBundle.message("package.dependencies.progress.title")

    private fun perform(
            builders: MutableList<MyDependenciesBuilder>,
            indicator: ProgressIndicator,
    ) {
        try {
            for (scope in scopes) {
                builders.add(MyForwardDependenciesBuilder(project, scope, myTransitiveBorder))
            }
            for (builder in builders) {
                builder.analyze()
            }
        } catch (e: IndexNotReadyException) {
            DumbService.getInstance(project).showDumbModeNotification(
                    CodeInsightBundle.message("analyze.dependencies.not.available.notification.indexing"))
            throw ProcessCanceledException()
        }
    }

    private fun refreshPanel(builders: MutableList<MyDependenciesBuilder>) {
        SwingUtilities.invokeLater {
            val displayName = CodeInsightBundle.message("package.dependencies.toolwindow.title", builders[0].scope.displayName)
            val panel = FileDependenciesPanel(project, builders, excluded)
            val content = ContentFactory.getInstance().createContent(panel, displayName, false)
            content.setDisposer(panel)
            panel.setContent(content)
            project.service<FileDependenciesToolWindow>().addContent(content)
        }
        ProjectView.getInstance(project).refresh()
    }
}