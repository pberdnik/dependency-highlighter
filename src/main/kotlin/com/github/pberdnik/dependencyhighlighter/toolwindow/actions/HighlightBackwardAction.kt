package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.icons.SdkIcons
import com.github.pberdnik.dependencyhighlighter.toolwindow.PluginSetting
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class HighlightBackwardAction(
    project: Project,
    private val rebuild: () -> Unit,
) : ToggleAction("Highlight Used By", "Highlight used by",
        SdkIcons.highlightBackward) {
    private val settings = project.service<PluginSetting>()
    override fun isSelected(event: AnActionEvent): Boolean {
        return settings.highlightBackward
    }

    override fun setSelected(event: AnActionEvent, flag: Boolean) {
        settings.highlightBackward = flag
        rebuild()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}