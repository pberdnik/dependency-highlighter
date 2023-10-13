package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.actions.MyForwardDependenciesBuilder
import com.github.pberdnik.dependencyhighlighter.toolwindow.MyDependenciesBuilder
import com.github.pberdnik.dependencyhighlighter.toolwindow.MyTree
import com.github.pberdnik.dependencyhighlighter.toolwindow.getSelectedScope
import com.intellij.analysis.AnalysisScope
import com.intellij.analysis.PerformAnalysisInBackgroundOption
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.psi.PsiFile

class AddToScopeAction(
        private val myProject: Project,
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val rebuild: () -> Unit,
        private val myLeftTree: MyTree,
        private val myRightTree: MyTree,
        private val myTransitiveBorder: Int,
        private val myBuilders: MutableList<MyDependenciesBuilder>,
        private val myDependencies: MutableMap<PsiFile, Set<PsiFile>>,
        private val myExcluded: MutableSet<PsiFile>,
        private val putAllDependencies: (builder: MyDependenciesBuilder) -> Unit,
        private val exclude: (excluded: Set<PsiFile>) -> Unit,
) : AnAction(ActionsBundle.messagePointer("action.AddToScopeAction.text")) {
    override fun update(e: AnActionEvent) {
        e.presentation.setEnabled(scope != null)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val scope = scope
        thisLogger().assertTrue(scope != null)
        val builder: MyDependenciesBuilder = MyForwardDependenciesBuilder(myProject, scope!!, myTransitiveBorder)
        val message = CodeInsightBundle.message("package.dependencies.progress.title")
        ProgressManager.getInstance().run(object : Task.Backgroundable(myProject, message, true, PerformAnalysisInBackgroundOption(myProject)) {
            override fun run(indicator: ProgressIndicator) {
                builder.analyze()
                myBuilders.add(builder)
                myDependencies.putAll(builder.dependencies)
                putAllDependencies(builder)
                exclude(myExcluded)
                rebuild()
            }
        })
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private val scope: AnalysisScope?
        get() {
            val selectedScope = getSelectedScope(myRightTree, mySettings.UI_FLATTEN_PACKAGES)
            val result: MutableSet<PsiFile> = HashSet()
            (myLeftTree.model.root as PackageDependenciesNode).fillFiles(result, !mySettings.UI_FLATTEN_PACKAGES)
            selectedScope.removeAll(result)
            if (selectedScope.isEmpty()) return null
            val files: MutableList<VirtualFile?> = ArrayList()
            val fileIndex = ProjectRootManager.getInstance(myProject).fileIndex
            for (psiFile in selectedScope) {
                val file = psiFile.virtualFile
                thisLogger().assertTrue(file != null)
                if (fileIndex.isInContent(file!!)) {
                    files.add(file)
                }
            }
            return if (files.isNotEmpty()) {
                AnalysisScope(myProject, files)
            } else null
        }
}