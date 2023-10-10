package com.github.pberdnik.dependencyhighlighter.panel.actions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.packageDependencies.DependencyUISettings
import com.intellij.packageDependencies.ui.DependenciesPanel

class GroupByScopeTypeAction(
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val rebuild: () -> Unit,
) : ToggleAction(CodeInsightBundle.messagePointer("action.group.by.scope.type"),
        CodeInsightBundle.messagePointer("action.group.by.scope.type.description"), AllIcons.Actions.GroupByTestProduction) {
    override fun isSelected(event: AnActionEvent): Boolean {
        return mySettings.UI_GROUP_BY_SCOPE_TYPE
    }

    override fun setSelected(event: AnActionEvent, flag: Boolean) {
        DependencyUISettings.getInstance().UI_GROUP_BY_SCOPE_TYPE = flag
        mySettings.UI_GROUP_BY_SCOPE_TYPE = flag
        rebuild()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}