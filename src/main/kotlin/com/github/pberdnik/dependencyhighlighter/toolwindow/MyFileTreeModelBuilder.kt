package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
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

class MyFileTreeModelBuilder(private val myProject: Project, marker: Marker?, settings: DependencyPanelSettings) {
    private val myFileIndex: ProjectFileIndex
    private val myShowModuleGroups: Boolean
    private val myFlattenPackages: Boolean
    private val myCompactEmptyMiddlePackages: Boolean
    private var myShowFiles: Boolean
    private val myMarker: Marker?
    private val myAddUnmarkedFiles: Boolean
    private val myRoot: PackageDependenciesNode
    private val myModuleDirNodes: MutableMap<DependencyType, MutableMap<VirtualFile, DirectoryNode>> = EnumMap(DependencyType::class.java)
    private val myModuleNodes: MutableMap<DependencyType, MutableMap<Module, ModuleNode>> = EnumMap(DependencyType::class.java)
    private val myModuleGroupNodes: MutableMap<DependencyType, MutableMap<String, ModuleGroupNode>> = EnumMap(DependencyType::class.java)
    private var myExternalNode: GeneralGroupNode? = null
    private val mForwardDependenciesNode: GeneralGroupNode
    private val mBackwardDependenciesNode: GeneralGroupNode
    private val mCycleDependenciesNode: GeneralGroupNode
    private var myScannedFileCount = 0
    private var myTotalFileCount = 0
    private var myMarkedFileCount = 0
    private var myTree: JTree? = null
    private val myBaseDir: VirtualFile = myProject.baseDir
    private var myContentRoots: Array<VirtualFile> = ProjectRootManager.getInstance(myProject).contentRoots

    init {
        val multiModuleProject = ModuleManager.getInstance(myProject).modules.size > 1
        val directoryHelper = ProjectViewDirectoryHelper.getInstance(myProject)
        myFlattenPackages = directoryHelper.supportsFlattenPackages() && settings.UI_FLATTEN_PACKAGES
        myCompactEmptyMiddlePackages = directoryHelper.supportsHideEmptyMiddlePackages() && settings.UI_COMPACT_EMPTY_MIDDLE_PACKAGES
        myShowFiles = settings.UI_SHOW_FILES
        myShowModuleGroups = settings.UI_SHOW_MODULE_GROUPS && multiModuleProject
        myMarker = marker
        myAddUnmarkedFiles = !settings.UI_FILTER_LEGALS
        myRoot = RootNode(myProject)
        myFileIndex = ProjectRootManager.getInstance(myProject).fileIndex
        myModuleNodes[DependencyType.FORWARD] = HashMap()
        myModuleNodes[DependencyType.BACKWARD] = HashMap()
        myModuleNodes[DependencyType.CYCLE] = HashMap()
        myModuleGroupNodes[DependencyType.FORWARD] = HashMap()
        myModuleGroupNodes[DependencyType.BACKWARD] = HashMap()
        myModuleGroupNodes[DependencyType.CYCLE] = HashMap()
        myModuleDirNodes[DependencyType.FORWARD] = HashMap()
        myModuleDirNodes[DependencyType.BACKWARD] = HashMap()
        myModuleDirNodes[DependencyType.CYCLE] = HashMap()
        mForwardDependenciesNode = GeneralGroupNode("Uses", AllIcons.General.ArrowRight, myProject)
        mBackwardDependenciesNode = GeneralGroupNode("Is used by", AllIcons.General.ArrowLeft, myProject)
        mCycleDependenciesNode = GeneralGroupNode("Cycle", AllIcons.General.Error, myProject)
        myRoot.add(mForwardDependenciesNode)
        myRoot.add(mBackwardDependenciesNode)
    }

    private fun build(forwardFiles: Set<VirtualFile>, backwardFiles: Set<VirtualFile>, cycleFiles: Set<VirtualFile>?, showProgress: Boolean): TreeModel {
        myShowFiles = true
        if (!cycleFiles.isNullOrEmpty()) {
            myRoot.add(mCycleDependenciesNode)
        }
        val buildingRunnable = Runnable {
            for (file in forwardFiles) {
                ReadAction.run<RuntimeException> { buildFileNode(file, DependencyType.FORWARD) }
            }
            for (file in backwardFiles) {
                ReadAction.run<RuntimeException> { buildFileNode(file, DependencyType.BACKWARD) }
            }
            for (file in cycleFiles!!) {
                ReadAction.run<RuntimeException> { buildFileNode(file, DependencyType.CYCLE) }
            }
        }
        if (showProgress) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(buildingRunnable, CodeInsightBundle
                    .message("package.dependencies.build.process.title"), false, myProject)
        } else {
            buildingRunnable.run()
        }
        TreeUtil.sortRecursively(mForwardDependenciesNode, DependencyNodeComparator())
        return TreeModel(myRoot, myTotalFileCount, myMarkedFileCount)
    }

    private fun buildFileNode(file: VirtualFile?, dependencyType: DependencyType): PackageDependenciesNode? {
        val indicator = ProgressManager.getInstance().progressIndicator
        if (file == null || !file.isValid) return null
        if (indicator != null) {
            update(indicator,  myScannedFileCount++.toDouble() / myTotalFileCount)
        }
        val isMarked = myMarker != null && myMarker.isMarked(file)
        if (isMarked) myMarkedFileCount++
        if (isMarked || myAddUnmarkedFiles) {
            val dirNode = getFileParentNode(file, dependencyType)
            if (myShowFiles) {
                val fileNode = FileNode(file, myProject, isMarked)
                dirNode.add(fileNode)
            } else {
                dirNode.addFile(file, isMarked)
            }
            return dirNode
        }
        return null
    }

    private fun getFileParentNode(file: VirtualFile?, dependencyType: DependencyType): PackageDependenciesNode {
        LOG.assertTrue(file != null)
        val containingDirectory = file!!.parent
        return getModuleDirNode(containingDirectory, myFileIndex.getModuleForFile(file), null, dependencyType)!!
    }

    private fun getModuleDirNode(virtualFile: VirtualFile?, module: Module?, childNode: DirectoryNode?, dependencyType: DependencyType): PackageDependenciesNode? {
        if (virtualFile == null) {
            return getModuleNode(dependencyType)
        }
        var directoryNode: PackageDependenciesNode? = myModuleDirNodes[dependencyType]!![virtualFile]
        if (directoryNode != null) {
            if (myCompactEmptyMiddlePackages) {
                val nestedNode = (directoryNode as DirectoryNode).compactedDirNode
                if (nestedNode != null) {
                    var expand = false
                    if (myTree != null) {
                        expand = !myTree!!.isCollapsed(TreePath(directoryNode.getPath()))
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
                    if (myTree != null && expand) {
                        val expandRunnable = Runnable { myTree!!.expandPath(TreePath(nestedNode.path)) }
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
        val fileIndex = ProjectRootManager.getInstance(myProject).fileIndex
        val sourceRoot = fileIndex.getSourceRootForFile(virtualFile)
        val contentRoot = fileIndex.getContentRootForFile(virtualFile)
        directoryNode = DirectoryNode(virtualFile, myProject, myCompactEmptyMiddlePackages, myFlattenPackages, myBaseDir,
                myContentRoots)
        myModuleDirNodes[dependencyType]!![virtualFile] = directoryNode
        val directory = virtualFile.parent
        if (!myFlattenPackages && directory != null) {
            if (myCompactEmptyMiddlePackages && !Comparing.equal(sourceRoot, virtualFile) && !Comparing.equal(contentRoot, virtualFile)) { //compact
                directoryNode.setCompactedDirNode(childNode)
            }
            if (fileIndex.getModuleForFile(directory) === module) {
                val parentDirectoryNode = myModuleDirNodes[dependencyType]!![directory]
                if (parentDirectoryNode != null || !myCompactEmptyMiddlePackages || sourceRoot != null && VfsUtilCore.isAncestor(directory, sourceRoot, false) && fileIndex.getSourceRootForFile(directory) != null
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
                val root: VirtualFile? = if (!Comparing.equal(sourceRoot, virtualFile) && sourceRoot != null) {
                    sourceRoot
                } else contentRoot
                if (root != null) {
                    getModuleDirNode(root, module, null, dependencyType)!!.add(directoryNode)
                } else {
                    if (myExternalNode == null) {
                        myExternalNode = GeneralGroupNode("External Dependencies", AllIcons.Nodes.PpLibFolder, myProject)
                        myRoot.add(myExternalNode)
                    }
                    myExternalNode!!.add(directoryNode)
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
            mForwardDependenciesNode
        } else if (dependencyType === DependencyType.BACKWARD) {
            mBackwardDependenciesNode
        } else if (dependencyType === DependencyType.CYCLE) {
            mCycleDependenciesNode
        } else {
            myExternalNode
        }
    }

    companion object {
        private val LOG = Logger.getInstance(MyFileTreeModelBuilder::class.java)
        private val FILE_COUNT = Key.create<Int>("FILE_COUNT")
        @Synchronized
        fun createTreeModel(project: Project, showProgress: Boolean, forwardFiles: Set<VirtualFile>, backwardFiles: Set<VirtualFile>, cycleFiles: Set<VirtualFile>?, marker: Marker?, settings: DependencyPanelSettings): TreeModel {
            return MyFileTreeModelBuilder(project, marker, settings).build(forwardFiles, backwardFiles, cycleFiles, showProgress)
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
