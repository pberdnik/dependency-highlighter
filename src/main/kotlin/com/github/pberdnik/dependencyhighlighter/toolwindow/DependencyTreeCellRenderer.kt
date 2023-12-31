package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.usageView.UsageViewBundle
import javax.swing.JTree

class DependencyTreeCellRenderer : ColoredTreeCellRenderer() {
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
            return
        }
        if (value.isValid) {
            icon = value.icon
        } else {
            append(UsageViewBundle.message("node.invalid") + " ", SimpleTextAttributes.ERROR_ATTRIBUTES)
        }
        append(
            value.toString(),
            if (value.hasMarked() && !selected) SimpleTextAttributes.ERROR_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES
        )
    }
}