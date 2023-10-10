package com.github.pberdnik.dependencyhighlighter.panel.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.packageDependencies.DependencyUISettings
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.packageDependencies.ui.PatternDialectProvider
import javax.swing.JComponent

class ChooseScopeTypeAction(
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val rebuild: () -> Unit,
) : ComboBoxAction() {
    override fun createPopupActionGroup(button: JComponent, context: DataContext): DefaultActionGroup {
        val group = DefaultActionGroup()
        for (provider in PatternDialectProvider.EP_NAME.extensionList) {
            group.add(object : AnAction(provider.displayName) {
                override fun actionPerformed(e: AnActionEvent) {
                    mySettings.SCOPE_TYPE = provider.shortName
                    DependencyUISettings.getInstance().SCOPE_TYPE = provider.shortName
                    rebuild()
                }
            })
        }
        return group
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val provider = PatternDialectProvider.getInstance(mySettings.SCOPE_TYPE)!!
        e.presentation.text = provider.displayName
        e.presentation.setIcon(provider.icon)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

}