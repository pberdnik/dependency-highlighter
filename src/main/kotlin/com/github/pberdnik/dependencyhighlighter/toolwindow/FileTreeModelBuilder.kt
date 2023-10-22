package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.ui.*
import com.intellij.packageDependencies.ui.DependenciesPanel.DependencyPanelSettings
import com.intellij.util.ui.tree.TreeUtil
import java.util.*
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

class FileTreeModelBuilder(
    private val project: Project,
    settings: DependencyPanelSettings,
    private val baseDir: VirtualFile,
) {
    private val fileIndex: ProjectFileIndex
    private val flattenPackages: Boolean
    private val compactEmptyMiddlePackages: Boolean
    private var showFiles: Boolean
    private val root: PackageDependenciesNode
    private val moduleDirNodes: MutableMap<DependencyType, MutableMap<VirtualFile, DirectoryNode>> = EnumMap(DependencyType::class.java)
    private val moduleNodes: MutableMap<DependencyType, MutableMap<Module, ModuleNode>> = EnumMap(DependencyType::class.java)
    private val moduleGroupNodes: MutableMap<DependencyType, MutableMap<String, ModuleGroupNode>> = EnumMap(DependencyType::class.java)
    private var externalNode: GeneralGroupNode? = null
    private val forwardDependenciesNode: GeneralGroupNode
    private val backwardDependenciesNode: GeneralGroupNode
    private var scannedFileCount = 0
    private var totalFileCount = 0
    private var jTree: JTree? = null
    private var contentRoots: Array<VirtualFile> = ProjectRootManager.getInstance(project).contentRoots

    init {
        val directoryHelper = ProjectViewDirectoryHelper.getInstance(project)
        flattenPackages = directoryHelper.supportsFlattenPackages() && settings.UI_FLATTEN_PACKAGES
        compactEmptyMiddlePackages = directoryHelper.supportsHideEmptyMiddlePackages() && settings.UI_COMPACT_EMPTY_MIDDLE_PACKAGES
        showFiles = settings.UI_SHOW_FILES
        root = RootNode(project)
        fileIndex = ProjectRootManager.getInstance(project).fileIndex
        moduleNodes[DependencyType.FORWARD] = HashMap()
        moduleNodes[DependencyType.BACKWARD] = HashMap()
        moduleNodes[DependencyType.CYCLE] = HashMap()
        moduleGroupNodes[DependencyType.FORWARD] = HashMap()
        moduleGroupNodes[DependencyType.BACKWARD] = HashMap()
        moduleGroupNodes[DependencyType.CYCLE] = HashMap()
        moduleDirNodes[DependencyType.FORWARD] = HashMap()
        moduleDirNodes[DependencyType.BACKWARD] = HashMap()
        moduleDirNodes[DependencyType.CYCLE] = HashMap()
        forwardDependenciesNode = GeneralGroupNode("Uses", AllIcons.General.ArrowRight, project)
        backwardDependenciesNode = GeneralGroupNode("Is used by", AllIcons.General.ArrowLeft, project)
        root.add(forwardDependenciesNode)
        root.add(backwardDependenciesNode)
    }

    private fun build(forwardFiles: Set<VirtualFile>, backwardFiles: Set<VirtualFile>, showProgress: Boolean): TreeModel {
        showFiles = true
        val buildingRunnable = Runnable {
            for (file in forwardFiles) {
                ReadAction.run<RuntimeException> { buildFileNode(file, DependencyType.FORWARD) }
            }
            for (file in backwardFiles) {
                ReadAction.run<RuntimeException> { buildFileNode(file, DependencyType.BACKWARD) }
            }
        }
        if (showProgress) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(buildingRunnable, "Scanning packages", false, project)
        } else {
            buildingRunnable.run()
        }
        TreeUtil.sortRecursively(forwardDependenciesNode, DependencyNodeComparator())
        return TreeModel(root, totalFileCount, 0)
    }

    private fun buildFileNode(file: VirtualFile?, dependencyType: DependencyType): PackageDependenciesNode? {
        val indicator = ProgressManager.getInstance().progressIndicator
        if (file == null || !file.isValid) return null
        if (indicator != null) {
            update(indicator,  scannedFileCount++.toDouble() / totalFileCount)
        }
        val dirNode = getFileParentNode(file, dependencyType)
        if (showFiles) {
            val fileNode = FileNode(file, project, false)
            dirNode.add(fileNode)
        } else {
            dirNode.addFile(file, false)
        }
        return dirNode
    }

    private fun getFileParentNode(file: VirtualFile?, dependencyType: DependencyType): PackageDependenciesNode {
        LOG.assertTrue(file != null)
        val containingDirectory = file!!.parent
        return getModuleDirNode(containingDirectory, fileIndex.getModuleForFile(file), null, dependencyType)!!
    }

    private fun getModuleDirNode(virtualFile: VirtualFile?, module: Module?, childNode: DirectoryNode?, dependencyType: DependencyType): PackageDependenciesNode? {
        if (virtualFile == null) {
            return getModuleNode(dependencyType)
        }
        var directoryNode: PackageDependenciesNode? = moduleDirNodes[dependencyType]!![virtualFile]
        if (directoryNode != null) {
            if (compactEmptyMiddlePackages) {
                val nestedNode = (directoryNode as DirectoryNode).compactedDirNode
                if (nestedNode != null) {
                    var expand = false
                    if (jTree != null) {
                        expand = !jTree!!.isCollapsed(TreePath(directoryNode.getPath()))
                    }
                    var parentWrapper = nestedNode.wrapper
                    while (parentWrapper.wrapper != null) {
                        parentWrapper = parentWrapper.wrapper
                    }
                    for (i in parentWrapper.childCount - 1 downTo 0) {
                        nestedNode.add(parentWrapper.getChildAt(i) as MutableTreeNode)
                    }
                    directoryNode.setCompactedDirNode(null)
                    parentWrapper.add(nestedNode)
                    nestedNode.removeUpReference()
                    if (jTree != null && expand) {
                        val expandRunnable = Runnable { jTree!!.expandPath(TreePath(nestedNode.path)) }
                        SwingUtilities.invokeLater(expandRunnable)
                    }
                    return parentWrapper
                }
                if (directoryNode.getParent() == null) {    //find first node in tree
                    var parentWrapper = directoryNode.wrapper
                    if (parentWrapper != null) {
                        while (parentWrapper!!.wrapper != null) {
                            parentWrapper = parentWrapper.wrapper
                        }
                        return parentWrapper
                    }
                }
            }
            return directoryNode
        }
        val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        val sourceRoot = projectFileIndex.getSourceRootForFile(virtualFile)
        val contentRoot = projectFileIndex.getContentRootForFile(virtualFile)
        directoryNode = DirectoryNode(virtualFile, project, compactEmptyMiddlePackages, flattenPackages, baseDir,
                contentRoots)
        moduleDirNodes[dependencyType]!![virtualFile] = directoryNode
        val directory = virtualFile.parent
        if (!flattenPackages && directory != null) {
            if (compactEmptyMiddlePackages && !Comparing.equal(sourceRoot, virtualFile) && !Comparing.equal(contentRoot, virtualFile)) { //compact
                directoryNode.setCompactedDirNode(childNode)
            }
            if (projectFileIndex.getModuleForFile(directory) === module) {
                val parentDirectoryNode = moduleDirNodes[dependencyType]!![directory]
                if (parentDirectoryNode != null || !compactEmptyMiddlePackages || sourceRoot != null && VfsUtilCore.isAncestor(directory, sourceRoot, false) && projectFileIndex.getSourceRootForFile(directory) != null
                        || Comparing.equal(directory, contentRoot)) {
                    getModuleDirNode(directory, module, directoryNode as DirectoryNode?, dependencyType)!!.add(directoryNode)
                } else {
                    directoryNode = getModuleDirNode(directory, module, directoryNode as DirectoryNode?, dependencyType)
                }
            } else {
                getModuleNode(dependencyType)!!.add(directoryNode)
            }
        } else {
            if (Comparing.equal<VirtualFile>(contentRoot, virtualFile)) {
                getModuleNode(dependencyType)!!.add(directoryNode)
            } else {
                val srcRoot: VirtualFile? = if (!Comparing.equal(sourceRoot, virtualFile) && sourceRoot != null) {
                    sourceRoot
                } else contentRoot
                if (srcRoot != null) {
                    getModuleDirNode(srcRoot, module, null, dependencyType)!!.add(directoryNode)
                } else {
                    if (externalNode == null) {
                        externalNode = GeneralGroupNode("External Dependencies", AllIcons.Nodes.PpLibFolder, project)
                        root.add(externalNode)
                    }
                    externalNode!!.add(directoryNode)
                }
            }
        }
        return directoryNode
    }

    private fun getModuleNode(dependencyType: DependencyType): PackageDependenciesNode? {
        return getMainNode(dependencyType)
    }

    private fun getMainNode(dependencyType: DependencyType): GeneralGroupNode? {
        return if (dependencyType === DependencyType.FORWARD) {
            forwardDependenciesNode
        } else if (dependencyType === DependencyType.BACKWARD) {
            backwardDependenciesNode
        } else {
            externalNode
        }
    }

    companion object {
        private val LOG = Logger.getInstance(FileTreeModelBuilder::class.java)
        private val FILE_COUNT = Key.create<Int>("FILE_COUNT")
        @Synchronized
        fun createTreeModel(project: Project, showProgress: Boolean, forwardFiles: Set<VirtualFile>, backwardFiles: Set<VirtualFile>, settings: DependencyPanelSettings, baseDir: VirtualFile): TreeModel {
            return FileTreeModelBuilder(project, settings, baseDir)
                .build(forwardFiles, backwardFiles, showProgress)
        }

        fun clearCaches(project: Project) {
            project.putUserData(FILE_COUNT, null)
        }

        private fun update(indicator: ProgressIndicator, fraction: Double) {
            if (indicator is PanelProgressIndicator) {
                val scanningPackagesMessage = CodeInsightBundle.message("package.dependencies.build.progress.text")
                indicator.update(scanningPackagesMessage, false, fraction)
            } else {
                if (fraction != -1.0) {
                    indicator.fraction = fraction
                }
            }
        }
    }
}
