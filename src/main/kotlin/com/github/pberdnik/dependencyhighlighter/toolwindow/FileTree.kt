package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.packageDependencies.ui.PackageDependenciesNode
import com.intellij.ui.treeStructure.Tree

class FileTree : Tree(), DataProvider {
    override fun getData(dataId: String): Any? {
        val node: PackageDependenciesNode? = selectedNode
        if (CommonDataKeys.NAVIGATABLE.`is`(dataId)) {
            return node
        }
        if (PlatformCoreDataKeys.BGT_DATA_PROVIDER.`is`(dataId)) {
            return getBackgroundDataProvider()
        }
        return null
    }

    private fun getBackgroundDataProvider(): DataProvider? {
        val node: PackageDependenciesNode? = selectedNode
        if (node != null) {
            return DataProvider { otherId ->
                if (CommonDataKeys.PSI_ELEMENT.`is`(otherId)) {
                    val element = node.psiElement
                    return@DataProvider if (element != null && element.isValid) element else null
                } else {
                    null
                }
            }

        }
        return null
    }

    val selectedNode: PackageDependenciesNode?
        get() {
            val paths = getSelectionPaths()
            return if (paths == null || paths.size != 1) null else paths[0].lastPathComponent as PackageDependenciesNode
        }
}
