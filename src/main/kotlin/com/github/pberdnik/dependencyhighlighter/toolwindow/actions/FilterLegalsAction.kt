package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.packageDependencies.DependencyUISettings
import com.intellij.packageDependencies.ui.DependenciesPanel


class FilterLegalsAction(
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val rebuild: () -> Unit,
        private val setEmptyText: (Boolean) -> Unit,
) : ToggleAction(CodeInsightBundle.messagePointer("action.show.illegals.only"),
        CodeInsightBundle.messagePointer("action.show.illegals.only.description"), AllIcons.General.Filter) {
    override fun isSelected(event: AnActionEvent): Boolean {
        return mySettings.UI_FILTER_LEGALS
    }

    override fun setSelected(event: AnActionEvent, flag: Boolean) {
        DependencyUISettings.getInstance().UI_FILTER_LEGALS = flag
        mySettings.UI_FILTER_LEGALS = flag
        setEmptyText(flag)
        rebuild()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}