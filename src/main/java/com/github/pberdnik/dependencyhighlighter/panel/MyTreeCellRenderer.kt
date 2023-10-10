package com.github.pberdnik.dependencyhighlighter.panel

import com.github.pberdnik.dependencyhighlighter.storage.GraphStorageService
import com.github.pberdnik.dependencyhighlighter.views.FileNodeView
import com.github.pberdnik.dependencyhighlighter.views.FileNodeViewColor
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.usageView.UsageViewBundle
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class MyTreeCellRenderer(
        private var mGraphStorageService: GraphStorageService
) : ColoredTreeCellRenderer() {
    override fun customizeCellRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
    ) {
        if (value !is PackageDependenciesNode) {
            thisLogger().error("value type should be PackageDependenciesNode but is " + value.javaClass + "; And value is " + (value as DefaultMutableTreeNode).path.contentToString())
            return
        }
        if (value.isValid) {
            icon = value.icon
        } else {
            append(UsageViewBundle.message("node.invalid") + " ", SimpleTextAttributes.ERROR_ATTRIBUTES)
        }
        append(value.toString(), if (value.hasMarked() && !selected) SimpleTextAttributes.ERROR_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES)
        val psiElement: PsiElement? = value.psiElement
        if (psiElement is PsiFile) {
            val path = psiElement.virtualFile.path
            val nodeView = mGraphStorageService.nodeViews[path]
            if (nodeView is FileNodeView) {
                var textColor = REGULAR_TEXT
                val fileNodeViewColor: FileNodeViewColor = nodeView.color
                if (fileNodeViewColor === FileNodeViewColor.GREEN) {
                    textColor = GREEN_TEXT
                } else if (fileNodeViewColor === FileNodeViewColor.RED) {
                    textColor = RED_TEXT
                } else if (fileNodeViewColor === FileNodeViewColor.YELLOW) {
                    textColor = YELLOW_TEXT
                } else if (fileNodeViewColor === FileNodeViewColor.GRAY) {
                    textColor = GRAY_TEXT
                }
                append(" " + nodeView.size + " [" + nodeView.depth + "]", textColor!!)
                if (nodeView.isCycle) append(" {C}", RED_TEXT)
            }
        }
    }

    companion object {
        private val REGULAR_TEXT = SimpleTextAttributes.REGULAR_ATTRIBUTES
        private val GREEN_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(JBColor.green.darker(), JBColor.green))
        private val RED_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(JBColor.red.darker(), JBColor.red))
        private val YELLOW_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(JBColor.yellow.darker(), JBColor.yellow))
        private val GRAY_TEXT = SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY)
    }
}