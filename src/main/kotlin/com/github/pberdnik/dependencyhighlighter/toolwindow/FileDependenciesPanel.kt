package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.actions.InFileHighlighter
import com.github.pberdnik.dependencyhighlighter.actions.MyAnalyzeDependenciesAction
import com.github.pberdnik.dependencyhighlighter.fileui.ProjectViewUiStateService
import com.github.pberdnik.dependencyhighlighter.toolwindow.actions.FlattenPackagesAction
import com.github.pberdnik.dependencyhighlighter.utils.EditorPsiElementHighlighter
import com.github.pberdnik.dependencyhighlighter.utils.UIUtils
import com.intellij.analysis.AnalysisScope
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
import com.intellij.packageDependencies.ui.*
import com.intellij.packageDependencies.ui.DependenciesPanel.DependencyPanelSettings
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
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
        myBuilders: MutableList<MyDependenciesBuilder>,
        myExcluded: MutableSet<PsiFile>
) : JPanel(BorderLayout()), Disposable, DataProvider {
    private val myDependencies: MutableMap<PsiFile, Set<PsiFile>>
    private val myBackwardDependencies: MutableMap<PsiFile, MutableSet<PsiFile>>
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
    private var mSelectedPsiFile: PsiFile? = null
    private var inFileHighlighter: InFileHighlighter? = null
    private val highlighter = EditorPsiElementHighlighter(project)

    init {
        myScopeOfInterest = null
        myTransitiveBorder = 0
        myDependencies = HashMap()
        myBackwardDependencies = HashMap()
        myIllegalDependencies = HashMap()
        for (builder in myBuilders) {
            myDependencies.putAll(builder.dependencies)
        }
        buildBackwardFromForwardDependencies()
        exclude(myExcluded)
        add(ScrollPaneFactory.createScrollPane(myRightTree), BorderLayout.CENTER)
        add(createToolbar(), BorderLayout.NORTH)
        myRightTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myRightTree, this.project)
        myRightTreeMarker = Marker { file -> myIllegalsInRightTree.contains(file) }
        updateRightTreeModel()
        val connection = this.project.messageBus.connect(this)
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(manager: FileEditorManager, file: VirtualFile) {
                mSelectedPsiFile = PsiManager.getInstance(project).findFile(file)
                mSelectedPsiFile?.let { selectedFile ->
                    inFileHighlighter = InFileHighlighter(project, selectedFile)
                    inFileHighlighter?.analyze()
                }
                updateRightTreeModel()
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                val newFile = event.newFile ?: return
                mSelectedPsiFile = PsiManager.getInstance(project).findFile(newFile)
                mSelectedPsiFile?.let { selectedFile ->
                    inFileHighlighter = InFileHighlighter(project, selectedFile)
                    inFileHighlighter?.analyze()
                }
                updateRightTreeModel()
            }
        })
        initTree(myRightTree)
        setEmptyText(mySettings.UI_FILTER_LEGALS)
    }

    private fun buildBackwardFromForwardDependencies() {
        myDependencies.forEach { (psiFile, depsSet) ->
            depsSet.forEach { psiDep ->
                if (myBackwardDependencies.contains(psiDep)) {
                    myBackwardDependencies[psiDep]?.add(psiFile)
                } else {
                    val set = hashSetOf(psiFile)
                    myBackwardDependencies[psiDep] = set
                }
            }
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
        group.add(MyAnalyzeDependenciesAction())
        group.add(FlattenPackagesAction(mySettings, ::rebuild))
        mySettings.UI_SHOW_FILES = true
        if (ModuleManager.getInstance(project).modules.size > 1) {
            mySettings.UI_SHOW_MODULES = true
            if (ModuleManager.getInstance(project).hasModuleGroups()) {
                mySettings.UI_SHOW_MODULE_GROUPS = true
            }
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true)
        toolbar.targetComponent = this
        return toolbar.component
    }

    private fun rebuild() {
        myIllegalDependencies = HashMap()
        updateRightTreeModel()
    }

    private fun initTree(tree: MyTree) {
        tree.setCellRenderer(MyTreeCellRenderer())
        tree.setRootVisible(false)
        tree.setShowsRootHandles(true)
        TreeUtil.installActions(tree)
        SmartExpander.installOn(tree)
        EditSourceOnDoubleClickHandler.install(tree)
        TreeSpeedSearch(tree)
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
            val depFiles = myDependencies[psiFile]?.map { it.virtualFile } ?: setOf()
            for (file in depFiles) {
                if (file.isValid) {
                    forwardDeps.add(file)
                }
            }
            val backDepFiles = myBackwardDependencies[psiFile]?.map { it.virtualFile } ?: setOf()
            for (file in backDepFiles) {
                if (file.isValid) {
                    backwardDeps.add(file)
                }
            }
        }
        forwardDeps.removeAll(vScope)
        backwardDeps.removeAll(vScope)
        project.service<ProjectViewUiStateService>().setDeps(forwardDeps, backwardDeps, cycleDeps)
        myRightTreeExpansionMonitor.freeze()
        myRightTree.setModel(buildTreeModel(forwardDeps, backwardDeps, cycleDeps, myRightTreeMarker))
        myRightTree.addTreeSelectionListener {
            highlighter.removeHighlight()
            val lastSelectedPathComponent = myRightTree.lastSelectedPathComponent
            if (lastSelectedPathComponent != null && lastSelectedPathComponent is FileNode) {
                val psiElement = lastSelectedPathComponent.psiElement
                if (psiElement is PsiFile) {
                    val deps = inFileHighlighter?.deps
                    deps?.forEach { dep ->
                        if (dep.psiFile.virtualFile.path == psiElement.virtualFile.path) {
                            highlighter.highlightElement(dep.psiElement)
                        }
                    }
                }
            }
        }
        myRightTreeExpansionMonitor.restore()
        expandFirstLevel(myRightTree)
        UIUtils.updateUI(project)
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
        if (PlatformCoreDataKeys.BGT_DATA_PROVIDER.`is`(dataId)) {
            return getBackgroundDataProvider()
        }
        return if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
            "dependency.viewer.tool.window"
        } else null
    }

    private fun getBackgroundDataProvider(): DataProvider? {
        val selectedNode: PackageDependenciesNode? = myRightTree.selectedNode
        if (selectedNode != null) {
            return DataProvider { otherId ->
                if (CommonDataKeys.PSI_ELEMENT.`is`(otherId)) {
                    val element = selectedNode.psiElement
                    return@DataProvider if (element != null && element.isValid) element else null
                } else {
                    null
                }
            }

        }
        return null
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
