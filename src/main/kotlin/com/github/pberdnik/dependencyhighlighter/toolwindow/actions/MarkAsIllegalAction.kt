package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.MyDependenciesBuilder
import com.github.pberdnik.dependencyhighlighter.toolwindow.MyTree
import com.github.pberdnik.dependencyhighlighter.toolwindow.getSelectedScope
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.packageDependencies.DependencyRule
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.packageDependencies.ui.PatternDialectProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.search.scope.packageSet.NamedScope

class MarkAsIllegalAction(
        private val myProject: Project,
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val rebuild: () -> Unit,
        private val myLeftTree: MyTree,
        private val myRightTree: MyTree,
        private val myTransitiveBorder: Int,
        private val myBuilders: MutableList<MyDependenciesBuilder>,
) : AnAction(CodeInsightBundle.messagePointer("mark.dependency.illegal.text"),
        CodeInsightBundle.messagePointer("mark.dependency.illegal.text"), AllIcons.Actions.Lightning) {
    override fun actionPerformed(e: AnActionEvent) {
        val leftNode: PackageDependenciesNode? = myLeftTree.selectedNode
        val rightNode: PackageDependenciesNode? = myRightTree.selectedNode
        if (leftNode != null && rightNode != null) {
            var hasDirectDependencies = myTransitiveBorder == 0
            if (myTransitiveBorder > 0) {
                val searchIn: Set<PsiFile> = getSelectedScope(myLeftTree, mySettings.UI_FLATTEN_PACKAGES)
                val searchFor: Set<PsiFile> = getSelectedScope(myRightTree, mySettings.UI_FLATTEN_PACKAGES)
                for (builder in myBuilders) {
                    if (hasDirectDependencies) break
                    for (from in searchIn) {
                        if (hasDirectDependencies) break
                        for (to in searchFor) {
                            if (hasDirectDependencies) break
                            val paths = builder.findPaths(from, to)
                            for (path in paths) {
                                if (path.isEmpty()) {
                                    hasDirectDependencies = true
                                    break
                                }
                            }
                        }
                    }
                }
            }
            val provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE)!!
            var leftPackageSet = provider.createPackageSet(leftNode, true)
            if (leftPackageSet == null) {
                leftPackageSet = provider.createPackageSet(leftNode, false)
            }
            thisLogger().assertTrue(leftPackageSet != null)
            var rightPackageSet = provider.createPackageSet(rightNode, true)
            if (rightPackageSet == null) {
                rightPackageSet = provider.createPackageSet(rightNode, false)
            }
            thisLogger().assertTrue(rightPackageSet != null)
            if (hasDirectDependencies) {
                DependencyValidationManager.getInstance(myProject)
                        .addRule(DependencyRule(NamedScope.UnnamedScope(leftPackageSet!!),
                                NamedScope.UnnamedScope(rightPackageSet!!), true))
                rebuild()
            } else {
                Messages.showErrorDialog(myProject, CodeInsightBundle
                        .message("analyze.dependencies.unable.to.create.rule.error.message", leftPackageSet!!.text, rightPackageSet!!.text),
                        CodeInsightBundle.message("mark.dependency.illegal.text"))
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.setEnabled(false)
        val leftNode: PackageDependenciesNode? = myLeftTree.selectedNode
        val rightNode: PackageDependenciesNode? = myRightTree.selectedNode
        if (leftNode != null && rightNode != null) {
            val provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE)!!
            presentation.setEnabled((provider.createPackageSet(leftNode, true) != null || provider.createPackageSet(leftNode, false) != null) &&
                    (provider.createPackageSet(rightNode, true) != null || provider.createPackageSet(rightNode, false) != null))
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}