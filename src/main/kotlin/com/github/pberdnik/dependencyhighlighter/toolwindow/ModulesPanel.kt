package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.actions.GraphAnalysisAction
import com.github.pberdnik.dependencyhighlighter.storage.GraphConfigStorageService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
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

class ModulesPanel(val project: Project) : JPanel(BorderLayout()) {
    private val graphConfig = project.service<GraphConfigStorageService>().state

    init {
        val modulesPanel = JPanel()
        modulesPanel.layout = BoxLayout(modulesPanel, BoxLayout.Y_AXIS)
        add(createToolbar(), BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(modulesPanel), BorderLayout.CENTER)
        ModuleManager.getInstance(project).modules.sortedBy { it.name }.forEach { module ->
            val name = module.name
            val checkBox = JBCheckBox(name, graphConfig.greenModules.contains(name))
            checkBox.addItemListener { itemEvent ->
                if (itemEvent.stateChange == ItemEvent.SELECTED) {
                    graphConfig.greenModules.add(name)
                } else if (itemEvent.stateChange == ItemEvent.DESELECTED) {
                    graphConfig.greenModules.remove(name)
                }
            }
            modulesPanel.add(checkBox)
        }
    }

    private fun createToolbar(): JComponent {
        val group = DefaultActionGroup()
        group.add(MyAnalyzeDependenciesAction())
        group.add(GraphAnalysisAction())
        val toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true)
        return toolbar.component
    }
}