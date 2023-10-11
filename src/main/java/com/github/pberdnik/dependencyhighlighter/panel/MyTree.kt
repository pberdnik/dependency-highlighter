package com.github.pberdnik.dependencyhighlighter.panel

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.psi.PsiFile
import com.intellij.ui.treeStructure.Tree
import java.util.HashSet

class MyTree : Tree(), DataProvider {
    override fun getData(dataId: String): Any? {
        val node: PackageDependenciesNode? = selectedNode
        if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
            return node
        }
        if (CommonDataKeys.PSI_ELEMENT.`is`(dataId) && node != null) {
            val element = node.psiElement
            return if (element != null && element.isValid) element else null
        }
        return null
    }

    val selectedNode: PackageDependenciesNode?
        get() {
            val paths = getSelectionPaths()
            return if (paths == null || paths.size != 1) null else paths[0].lastPathComponent as PackageDependenciesNode
        }
}

fun getSelectedScope(tree: Tree, flattenPackages: Boolean): MutableSet<PsiFile> {
    val paths = tree.getSelectionPaths() ?: return EMPTY_FILE_SET
    val result: MutableSet<PsiFile> = HashSet()
    for (path in paths) {
        val node = path.lastPathComponent as PackageDependenciesNode
        node.fillFiles(result, !flattenPackages)
    }
    return result
}

private val EMPTY_FILE_SET: MutableSet<PsiFile> = HashSet(0)