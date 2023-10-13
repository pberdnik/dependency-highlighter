package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.MyTree
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.DependencyUISettings
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.util.*
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class SelectInLeftTreeAction(
        private val myProject: Project,
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val myLeftTree: MyTree,
        private val myRightTree: MyTree,
        private val myDependencies: MutableMap<PsiFile, Set<PsiFile>>,
) : AnAction(CodeInsightBundle.messagePointer("action.select.in.left.tree"),
        CodeInsightBundle.messagePointer("action.select.in.left.tree.description"), null) {
    override fun update(e: AnActionEvent) {
        val node: PackageDependenciesNode? = myRightTree.selectedNode
        e.presentation.setEnabled(node != null && node.canSelectInLeftTree(myDependencies))
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun actionPerformed(e: AnActionEvent) {
        val node: PackageDependenciesNode? = myRightTree.selectedNode
        if (node != null) {
            val elt = node.psiElement
            if (elt != null) {
                DependencyUISettings.getInstance().UI_FILTER_LEGALS = false
                mySettings.UI_FILTER_LEGALS = false
                selectElementInLeftTree(elt)
            }
        }
    }

    private fun selectElementInLeftTree(elt: PsiElement) {
        val manager = PsiManager.getInstance(myProject)
        val root = myLeftTree.model.root as PackageDependenciesNode
        val enumeration: Enumeration<*> = root.breadthFirstEnumeration()
        while (enumeration.hasMoreElements()) {
            val child = enumeration.nextElement() as PackageDependenciesNode
            if (manager.areElementsEquivalent(child.psiElement, elt)) {
                myLeftTree.selectionPath = TreePath((myLeftTree.model as DefaultTreeModel).getPathToRoot(child))
                break
            }
        }
    }
}