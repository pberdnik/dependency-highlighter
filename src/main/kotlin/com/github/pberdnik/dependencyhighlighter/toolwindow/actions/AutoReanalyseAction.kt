package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.icons.SdkIcons
import com.github.pberdnik.dependencyhighlighter.toolwindow.PluginSetting
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class AutoReanalyseAction(
    project: Project,
    private val rebuild: () -> Unit,
) : ToggleAction("Auto Reanalyse", "Track file changes events and reanalyse automatically",
        SdkIcons.autoReanalyse) {
    private val settings = project.service<PluginSetting>()
    override fun isSelected(event: AnActionEvent): Boolean {
        return settings.autoReanalyse
    }

    override fun setSelected(event: AnActionEvent, flag: Boolean) {
        settings.autoReanalyse = flag
        rebuild()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}