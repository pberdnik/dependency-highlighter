package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.packageDependencies.DependencyUISettings
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.util.PlatformIcons

class FlattenPackagesAction(
    private val settings: DependenciesPanel.DependencyPanelSettings,
    private val rebuild: () -> Unit,
) : ToggleAction(CodeInsightBundle.messagePointer("action.flatten.packages"), CodeInsightBundle.messagePointer("action.flatten.packages"),
        PlatformIcons.FLATTEN_PACKAGES_ICON) {
    override fun isSelected(event: AnActionEvent): Boolean {
        return settings.UI_FLATTEN_PACKAGES
    }

    override fun setSelected(event: AnActionEvent, flag: Boolean) {
        DependencyUISettings.getInstance().UI_FLATTEN_PACKAGES = flag
        settings.UI_FLATTEN_PACKAGES = flag
        rebuild()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}