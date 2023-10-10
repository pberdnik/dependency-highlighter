package com.github.pberdnik.dependencyhighlighter.panel.actions

import com.github.pberdnik.dependencyhighlighter.panel.MyTree
import com.github.pberdnik.dependencyhighlighter.panel.getSelectedScope
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.psi.PsiFile
import com.intellij.util.ui.tree.TreeUtil

class RemoveFromScopeAction(
        private val myExcluded: MutableSet<PsiFile>,
        private val exclude: (excluded: Set<PsiFile>) -> Unit,
        private val myLeftTree: MyTree,
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
) : AnAction(ActionsBundle.messagePointer("action.RemoveFromScopeAction.text")) {
    override fun update(e: AnActionEvent) {
        e.presentation.setEnabled(!getSelectedScope(myLeftTree, mySettings.UI_FLATTEN_PACKAGES).isEmpty())
    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedScope: Set<PsiFile> = getSelectedScope(myLeftTree, mySettings.UI_FLATTEN_PACKAGES)
        exclude(selectedScope)
        myExcluded.addAll(selectedScope)
        val paths = myLeftTree.getSelectionPaths()!!
        for (path in paths) {
            TreeUtil.removeLastPathComponent(myLeftTree, path!!)
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}