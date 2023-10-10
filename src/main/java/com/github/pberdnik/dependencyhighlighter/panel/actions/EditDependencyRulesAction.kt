package com.github.pberdnik.dependencyhighlighter.panel.actions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.ui.DependencyConfigurable

class EditDependencyRulesAction(
        private val myProject: Project,
        private val rebuild: () -> Unit,
) : AnAction(CodeInsightBundle.messagePointer("action.edit.rules"), CodeInsightBundle.messagePointer("action.edit.rules.description"),
        AllIcons.General.Settings) {
    override fun actionPerformed(e: AnActionEvent) {
        val applied = ShowSettingsUtil.getInstance().editConfigurable(myProject, DependencyConfigurable(myProject))
        if (applied) {
            rebuild()
        }
    }
}