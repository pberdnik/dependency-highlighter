// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.pberdnik.dependencyhighlighter.panel

import com.github.pberdnik.dependencyhighlighter.panel.actions.*
import com.github.pberdnik.dependencyhighlighter.fileui.ProjectViewUiStateService
import com.github.pberdnik.dependencyhighlighter.storage.GraphStorageService
import com.github.pberdnik.dependencyhighlighter.utils.UIUtils
import com.intellij.analysis.AnalysisScope
import com.intellij.ide.impl.FlattenModulesToggleAction
import com.intellij.lang.LangBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.DependencyRule
import com.intellij.packageDependencies.DependencyUISettings
import com.intellij.packageDependencies.MyDependenciesBuilder
import com.intellij.packageDependencies.actions.MyBackwardDependenciesBuilder
import com.intellij.packageDependencies.actions.MyForwardDependenciesBuilder
import com.intellij.packageDependencies.ui.*
import com.intellij.packageDependencies.ui.DependenciesPanel.DependencyPanelSettings
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SmartExpander
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.content.Content
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.TreePath

class FileDependenciesPanel(
        private val project: Project,
        private val myBuilders: MutableList<MyDependenciesBuilder>,
        private val myExcluded: MutableSet<PsiFile>
) : JPanel(BorderLayout()), Disposable, DataProvider {
    private val myDependencies: MutableMap<PsiFile, Set<PsiFile>>
    private var myIllegalDependencies: MutableMap<VirtualFile, MutableMap<DependencyRule, MutableSet<PsiFile>>?>
    private val myLeftTree = MyTree()
    private val myRightTree = MyTree()
    private val myRightTreeExpansionMonitor: TreeExpansionMonitor<*>
    private val myRightTreeMarker: Marker
    private var myIllegalsInRightTree: MutableSet<VirtualFile> = HashSet()
    private var myContent: Content? = null
    private val mySettings = DependencyPanelSettings()
    private val myScopeOfInterest: AnalysisScope?
    private val myTransitiveBorder: Int
    private val mGraphStorageService: GraphStorageService
    private var mSelectedPsiFile: PsiFile? = null

    init {
        val main = myBuilders[0]
        myScopeOfInterest = if (main is MyBackwardDependenciesBuilder) main.scopeOfInterest else null
        myTransitiveBorder = if (main is MyForwardDependenciesBuilder) main.transitiveBorder else 0
        myDependencies = HashMap()
        myIllegalDependencies = HashMap()
        for (builder in myBuilders) {
            myDependencies.putAll(builder.dependencies)
            putAllDependencies(builder)
        }
        exclude(myExcluded)
        mGraphStorageService = project.getService(GraphStorageService::class.java)
        add(ScrollPaneFactory.createScrollPane(myRightTree), BorderLayout.CENTER)
        add(createToolbar(), BorderLayout.NORTH)
        myRightTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myRightTree, this.project)
        myRightTreeMarker = Marker { file -> myIllegalsInRightTree.contains(file) }
        updateRightTreeModel()
        val connection = this.project.messageBus.connect(this)
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(manager: FileEditorManager, file: VirtualFile) {
                mSelectedPsiFile = PsiManager.getInstance(project).findFile(file)
                updateRightTreeModel()
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                mSelectedPsiFile = PsiManager.getInstance(project).findFile(event.newFile)
                updateRightTreeModel()
            }
        })
        initTree(myRightTree)
        setEmptyText(mySettings.UI_FILTER_LEGALS)
    }

    private fun putAllDependencies(builder: MyDependenciesBuilder) {
        val dependencies = builder.getIllegalDependencies()
        for ((key, value) in dependencies) {
            myIllegalDependencies[key.virtualFile] = value
        }
    }

    private fun exclude(excluded: Set<PsiFile>) {
        for (psiFile in excluded) {
            myDependencies.remove(psiFile)
            myIllegalDependencies.remove(psiFile.virtualFile)
        }
    }

    private fun createToolbar(): JComponent {
        val group = DefaultActionGroup()
        group.add(CloseAction(project, mySettings, myContent))
        group.add(FlattenPackagesAction(mySettings, ::rebuild))
        mySettings.UI_SHOW_FILES = true
        if (ModuleManager.getInstance(project).modules.size > 1) {
            mySettings.UI_SHOW_MODULES = true
            group.add(createFlattenModulesAction())
            if (ModuleManager.getInstance(project).hasModuleGroups()) {
                mySettings.UI_SHOW_MODULE_GROUPS = true
            }
        }
        group.add(GroupByScopeTypeAction(mySettings, ::rebuild))
        group.add(FilterLegalsAction(mySettings, ::rebuild, ::setEmptyText))
        group.add(MarkAsIllegalAction(project, mySettings, ::rebuild, myLeftTree, myRightTree, myTransitiveBorder, myBuilders))
        group.add(ChooseScopeTypeAction(mySettings, ::rebuild))
        group.add(EditDependencyRulesAction(project, ::rebuild))
        val toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true)
        return toolbar.component
    }

    private fun createFlattenModulesAction(): FlattenModulesToggleAction {
        return FlattenModulesToggleAction(project, { mySettings.UI_SHOW_MODULES }, { !mySettings.UI_SHOW_MODULE_GROUPS }) { value: Boolean? ->
            DependencyUISettings.getInstance().UI_SHOW_MODULE_GROUPS = !value!!
            mySettings.UI_SHOW_MODULE_GROUPS = !value
            rebuild()
        }
    }

    private fun rebuild() {
        myIllegalDependencies = HashMap()
        for (builder in myBuilders) {
            putAllDependencies(builder)
        }
        updateRightTreeModel()
    }

    private fun initTree(tree: MyTree) {
        tree.setCellRenderer(MyTreeCellRenderer(mGraphStorageService))
        tree.setRootVisible(false)
        tree.setShowsRootHandles(true)
        TreeUtil.installActions(tree)
        SmartExpander.installOn(tree)
        EditSourceOnDoubleClickHandler.install(tree)
        TreeSpeedSearch(tree)
        PopupHandler.installPopupMenu(tree, createTreePopupActions(), "DependenciesPopup")
    }

    private fun updateRightTreeModel() {
        val forwardDeps: MutableSet<VirtualFile> = HashSet()
        val backwardDeps: MutableSet<VirtualFile> = HashSet()
        val cycleDeps: MutableSet<VirtualFile> = HashSet()
        val scope: MutableSet<PsiFile> = HashSet()
        val vScope: MutableSet<VirtualFile> = HashSet()
        mSelectedPsiFile?.let {
            scope.add(it)
            vScope.add(it.virtualFile)
        } ?: return
        myIllegalsInRightTree = HashSet()
        for (psiFile in scope) {
            val illegalDeps = myIllegalDependencies[psiFile.virtualFile]
            if (illegalDeps != null) {
                for (rule in illegalDeps.keys) {
                    val files = illegalDeps[rule]!!
                    for (file in files) {
                        myIllegalsInRightTree.add(file.virtualFile)
                    }
                }
            }
            val forwardFiles = mGraphStorageService.getForwardDepsForPath(psiFile.virtualFile.path) //
            for (file in forwardFiles) {
                if (file.isValid) {
                    forwardDeps.add(file)
                }
            }
            val backwardFiles = mGraphStorageService.getBackwardDepsForPath(psiFile.virtualFile.path) //
            for (file in backwardFiles) {
                if (file.isValid) {
                    backwardDeps.add(file)
                }
            }
            val cycleFiles = mGraphStorageService.getCycleDepsForPath(psiFile.virtualFile.path) //
            for (file in cycleFiles) {
                if (file.isValid) {
                    cycleDeps.add(file)
                }
            }
        }
        forwardDeps.removeAll(vScope)
        backwardDeps.removeAll(vScope)
        project.service<ProjectViewUiStateService>().setDeps(forwardDeps, backwardDeps, cycleDeps)
        myRightTreeExpansionMonitor.freeze()
        myRightTree.setModel(buildTreeModel(forwardDeps, backwardDeps, cycleDeps, myRightTreeMarker))
        myRightTreeExpansionMonitor.restore()
        expandFirstLevel(myRightTree)
        UIUtils.updateUI(project)
    }

    private fun createTreePopupActions(): ActionGroup {
        val group = DefaultActionGroup()
        val actionManager = ActionManager.getInstance()
        group.add(actionManager.getAction(IdeActions.ACTION_EDIT_SOURCE))
        group.add(actionManager.getAction(IdeActions.GROUP_VERSION_CONTROLS))
        group.add(actionManager.getAction(IdeActions.GROUP_ANALYZE))
        group.add(AddToScopeAction(project, mySettings, ::rebuild, myLeftTree, myRightTree, myTransitiveBorder, myBuilders, myDependencies, myExcluded, ::putAllDependencies, ::exclude))
        group.add(ShowDetailedInformationAction(mySettings, myLeftTree, myRightTree, myTransitiveBorder, myBuilders))
        return group
    }

    private fun buildTreeModel(forwardDeps: Set<VirtualFile>, backwardDeps: Set<VirtualFile>, cycleDeps: Set<VirtualFile>, marker: Marker): TreeModel {
        return MyFileTreeModelBuilder.createTreeModel(project, false, forwardDeps, backwardDeps, cycleDeps, marker, mySettings)
    }

    fun setContent(content: Content?) {
        myContent = content
    }

    override fun dispose() {
        MyFileTreeModelBuilder.clearCaches(project)
    }

    override fun getData(dataId: @NonNls String): @NonNls Any? {
        if (CommonDataKeys.PSI_ELEMENT.`is`(dataId)) {
            val selectedNode: PackageDependenciesNode? = myRightTree.selectedNode
            if (selectedNode != null) {
                val element = selectedNode.psiElement
                return if (element != null && element.isValid) element else null
            }
        }
        return if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
            "dependency.viewer.tool.window"
        } else null
    }

    private fun setEmptyText(flag: Boolean) {
        val emptyText = if (flag) LangBundle.message("status.text.no.illegal.dependencies.found") else LangBundle.message("status.text.nothing.to.show")
        myLeftTree.emptyText.setText(emptyText)
        myRightTree.emptyText.setText(emptyText)
    }

    companion object {
        private fun expandFirstLevel(tree: Tree) {
            val root = tree.model.root as PackageDependenciesNode
            val count = root.childCount
            if (count < 10) {
                for (i in 0 until count) {
                    val child = root.getChildAt(i) as PackageDependenciesNode
                    expandNodeIfNotTooWide(tree, child)
                }
            }
        }

        private fun expandNodeIfNotTooWide(tree: Tree, node: PackageDependenciesNode) {
            val count = node.childCount
            if (count > 5) return
            //another level of nesting
            if (count == 1 && node.getChildAt(0).childCount > 5) {
                return
            }
            tree.expandPath(TreePath(node.path))
        }
    }
}
