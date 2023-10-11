package com.github.pberdnik.dependencyhighlighter.fileui

import com.github.pberdnik.dependencyhighlighter.storage.GraphStorageService
import com.github.pberdnik.dependencyhighlighter.views.DirNodeView
import com.github.pberdnik.dependencyhighlighter.views.FileNodeView
import com.github.pberdnik.dependencyhighlighter.views.FileNodeViewColor
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes

private val REGULAR_TEXT = SimpleTextAttributes.REGULAR_ATTRIBUTES
private val GREEN_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(JBColor.green.darker(), JBColor.green))
private val RED_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(JBColor.red.darker(), JBColor.red))
private val YELLOW_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(JBColor.yellow.darker(), JBColor.yellow))
private val GRAY_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY)

class DependenciesProjectViewNodeDecorator(val project: Project) : ProjectViewNodeDecorator {

    private val uiService = project.service<ProjectViewUiStateService>()

    override fun decorate(node: ProjectViewNode<*>?, data: PresentationData?) {
        if (node == null || data == null) return
        val value = node.value ?: return
        val file = node.virtualFile ?: PsiUtilCore.getVirtualFile(value as? PsiElement)
        val path = file?.path ?: return
        data.clearText()
        data.presentableText = ""
        data.addText(file.name, REGULAR_TEXT)
        val forwardDepsNum = uiService.forwardDepsNumMap[path] ?: 0
        if (forwardDepsNum > 0) {
            data.addText(" $forwardDepsNum", GREEN_TEXT)
        }
        val backwardDepsNum = uiService.backwardDepsNumMap[path] ?: 0
        if (backwardDepsNum > 0) {
            data.addText(" $backwardDepsNum", RED_TEXT)
        }
    }
}