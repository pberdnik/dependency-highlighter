package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.FilesChangeListener
import com.github.pberdnik.dependencyhighlighter.actions.AnalyzeDependenciesAction
import com.github.pberdnik.dependencyhighlighter.actions.InFileHighlighter
import com.github.pberdnik.dependencyhighlighter.fileui.ProjectViewUiStateService
import com.github.pberdnik.dependencyhighlighter.toolwindow.actions.*
import com.github.pberdnik.dependencyhighlighter.utils.EditorPsiElementHighlighter
import com.github.pberdnik.dependencyhighlighter.utils.UIUtils
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.lang.LangBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.packageDependencies.ui.*
import com.intellij.packageDependencies.ui.DependenciesPanel.DependencyPanelSettings
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SmartExpander
import com.intellij.ui.TreeSpeedSearch
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
) : JPanel(BorderLayout()), Disposable, DataProvider {

    private val dependenciesHandler = project.service<DependenciesHandlerService>()
    private val rightTree = FileTree()
    private val rightTreeExpansionMonitor: TreeExpansionMonitor<*>
    private val settings = DependencyPanelSettings()
    private var selectedPsiFile: PsiFile? = null
    private var inFileHighlighter: InFileHighlighter? = null
    private val highlighter = EditorPsiElementHighlighter(project)
    private val pluginSettings = project.service<PluginSetting>()

    init {
        add(ScrollPaneFactory.createScrollPane(rightTree), BorderLayout.CENTER)
        add(createToolbar(), BorderLayout.NORTH)
        rightTreeExpansionMonitor = PackageTreeExpansionMonitor.install(rightTree, this.project)
        updateRightTreeModel()
        val connection = this.project.messageBus.connect(this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, FilesChangeListener(project))
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(manager: FileEditorManager, file: VirtualFile) {
                selectedPsiFile = PsiManager.getInstance(project).findFile(file)
                selectedPsiFile?.let { selectedFile ->
                    inFileHighlighter = InFileHighlighter(project, selectedFile)
                    inFileHighlighter?.analyze()
                }
                updateRightTreeModel()
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                try {
                    val newFile = event.newFile ?: return
                    selectedPsiFile = PsiManager.getInstance(project).findFile(newFile)
                    selectedPsiFile?.let { selectedFile ->
                        inFileHighlighter = InFileHighlighter(project, selectedFile)
                        inFileHighlighter?.analyze()
                    }
                    updateRightTreeModel()
                } catch (e: IndexNotReadyException) {
                    DumbService.getInstance(project).showDumbModeNotification(
                        CodeInsightBundle.message("analyze.dependencies.not.available.notification.indexing"))
                    throw ProcessCanceledException()
                }
            }
        })
        initTree(rightTree)
        rightTree.emptyText.setText(LangBundle.message("status.text.nothing.to.show"))
    }

    private fun createToolbar(): JComponent {
        val group = DefaultActionGroup()
        group.add(AnalyzeDependenciesAction())
        group.add(RerunAnalysisAction(project, ::updateRightTreeModel))
        group.add(AutoReanalyseAction(project, ::updateRightTreeModel))
        group.add(FlattenPackagesAction(settings, ::updateRightTreeModel))
        group.add(HighlightForwardAction(project, ::updateRightTreeModel))
        group.add(HighlightBackwardAction(project, ::updateRightTreeModel))
        group.add(ChooseBaseDirAction(project, ::updateRightTreeModel))
        settings.UI_SHOW_FILES = true
        if (ModuleManager.getInstance(project).modules.size > 1) {
            settings.UI_SHOW_MODULES = true
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("PackageDependencies", group, true)
        toolbar.targetComponent = this
        return toolbar.component
    }

    private fun initTree(tree: FileTree) {
        tree.setCellRenderer(DependencyTreeCellRenderer())
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
        val scope: MutableSet<PsiFile> = HashSet()
        selectedPsiFile?.let {
            scope.add(it)
        } ?: return
        for (psiFile in scope) {
            val depFiles = dependenciesHandler.getForwardDependencies(psiFile.virtualFile)
            for (file in depFiles) {
                if (file.isValid) {
                    forwardDeps.add(file)
                }
            }
            val backDepFiles = dependenciesHandler.getBackwardDependencies(psiFile.virtualFile)
            for (file in backDepFiles) {
                if (file.isValid) {
                    backwardDeps.add(file)
                }
            }
        }
        forwardDeps.remove(selectedPsiFile?.virtualFile)
        backwardDeps.remove(selectedPsiFile?.virtualFile)
        project.service<ProjectViewUiStateService>().setDeps(forwardDeps, backwardDeps)
        rightTreeExpansionMonitor.freeze()
        val baseDirPath = pluginSettings.baseDir
        val baseDir = if (baseDirPath != null) {
            LocalFileSystem.getInstance().findFileByPath(baseDirPath)
        } else {
            project.guessProjectDir()
        }
        if (baseDir == null) {
            UIUtils.updateUI(project)
            return
        }
        rightTree.setModel(buildTreeModel(forwardDeps, backwardDeps, baseDir))
        rightTree.addTreeSelectionListener {
            highlighter.removeHighlight()
            val lastSelectedPathComponent = rightTree.lastSelectedPathComponent
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
        rightTreeExpansionMonitor.restore()
        expandFirstLevel(rightTree)
        UIUtils.updateUI(project)
    }

    private fun buildTreeModel(forwardDeps: Set<VirtualFile>, backwardDeps: Set<VirtualFile>, baseDir: VirtualFile): TreeModel {
        return FileTreeModelBuilder.createTreeModel(project, false, forwardDeps, backwardDeps, settings, baseDir)
    }

    override fun dispose() {
        FileTreeModelBuilder.clearCaches(project)
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
        val selectedNode: PackageDependenciesNode? = rightTree.selectedNode
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
