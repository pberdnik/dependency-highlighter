package com.intellij.packageDependencies.actions

import com.github.pberdnik.dependencyhighlighter.MyBundle
import com.github.pberdnik.dependencyhighlighter.panel.FileAnalyzeDependenciesHandler
import com.intellij.analysis.*
import com.intellij.analysis.dialog.ModelScopeItem
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInspection.ui.InspectionResultsView
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import icons.SdkIcons
import javax.swing.JComponent

class MyAnalyzeDependenciesAction : AnAction("Run Analysis", "Run analysis", AllIcons.Actions.Rerun) {
    fun analyze(project: Project, scope: AnalysisScope) {
        FileAnalyzeDependenciesHandler(project, listOf(scope), 0).analyze()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dataContext = e.dataContext
        var scope = getInspectionScope(dataContext, project) ?: return
        val module = getModuleFromContext(dataContext)
        val rememberScope = ActionPlaces.isMainMenuOrActionSearch(e.place)
        val uiOptions = AnalysisUIOptions.getInstance(project)
        val element = CommonDataKeys.PSI_ELEMENT.getData(dataContext)
        val items = BaseAnalysisActionDialog.standardItems(project, scope, module, element)
        val dlg = getAnalysisDialog(project, rememberScope, uiOptions, items)
        if (!dlg.showAndGet()) {
            return
        }
        val oldScopeType = uiOptions.SCOPE_TYPE
        scope = dlg.getScope(scope)
        if (!rememberScope) {
            uiOptions.SCOPE_TYPE = oldScopeType
        }
        uiOptions.ANALYZE_TEST_SOURCES = dlg.isInspectTestSources
        FileDocumentManager.getInstance().saveAllDocuments()
        analyze(project, scope)
    }

    private fun getAnalysisDialog(project: Project,
                          rememberScope: Boolean,
                          uiOptions: AnalysisUIOptions,
                          items: List<ModelScopeItem?>): BaseAnalysisActionDialog {
        return object : BaseAnalysisActionDialog("Specify Analysis Scope", "Analysis Scope", project, items, uiOptions, rememberScope) {
            override fun getHelpId() = "reference.dialogs.analyzeDependencies.scope"
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.setEnabled(project != null && !DumbService.isDumb(project) && getInspectionScope(e.dataContext, project) != null)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    private fun getInspectionScope(dataContext: DataContext, project: Project): AnalysisScope? {
        return AnalysisActionUtils.getInspectionScope(dataContext, project, false)
    }

    companion object {
        private fun getModuleFromContext(dataContext: DataContext): Module? {
            val inspectionView = dataContext.getData(InspectionResultsView.DATA_KEY)
            if (inspectionView != null) {
                val scope = inspectionView.scope
                if (scope.scopeType == AnalysisScope.MODULE && scope.isValid()) {
                    return scope.module
                }
            }
            return dataContext.getData(PlatformCoreDataKeys.MODULE)
        }
    }
}
