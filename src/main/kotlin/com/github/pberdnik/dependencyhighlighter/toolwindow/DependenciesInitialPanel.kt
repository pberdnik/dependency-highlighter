package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.actions.GraphAnalysisAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.packageDependencies.actions.MyAnalyzeDependenciesAction
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBCheckBox
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class DependenciesInitialPanel(val project: Project) : JPanel(BorderLayout()) {

    init {
        val initialPanel = JPanel()
        initialPanel.layout = BoxLayout(initialPanel, BoxLayout.Y_AXIS)
        add(createToolbar(), BorderLayout.NORTH)
    }

    private fun createToolbar(): JComponent {
        val group = DefaultActionGroup()
        group.add(MyAnalyzeDependenciesAction())
        val toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true)
        return toolbar.component
    }
}