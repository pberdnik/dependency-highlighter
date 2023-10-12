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

class MyTreeCellRenderer: ColoredTreeCellRenderer() {
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
    }
}