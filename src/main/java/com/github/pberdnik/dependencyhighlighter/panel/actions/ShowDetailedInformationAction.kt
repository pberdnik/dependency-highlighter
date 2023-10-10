package com.github.pberdnik.dependencyhighlighter.panel.actions

import com.github.pberdnik.dependencyhighlighter.panel.MyTree
import com.github.pberdnik.dependencyhighlighter.panel.getSelectedScope
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.idea.ActionsBundle
import com.intellij.lang.LangBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.packageDependencies.MyDependenciesBuilder
import com.intellij.packageDependencies.ui.DependenciesPanel
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.Processor
import com.intellij.util.ui.UIUtil
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.annotations.NonNls
import java.awt.Dimension
import javax.swing.JEditorPane

class ShowDetailedInformationAction(
        private val mySettings: DependenciesPanel.DependencyPanelSettings,
        private val myLeftTree: MyTree,
        private val myRightTree: MyTree,
        private val myTransitiveBorder: Int,
        private val myBuilders: MutableList<MyDependenciesBuilder>,
) : AnAction(ActionsBundle.messagePointer("action.ShowDetailedInformationAction.text")) {
    override fun actionPerformed(e: AnActionEvent) {
        val delim: @NonNls String = "&nbsp;-&gt;&nbsp;"
        val buf = StringBuffer()
        processDependencies(getSelectedScope(myLeftTree, mySettings.UI_FLATTEN_PACKAGES), getSelectedScope(myRightTree, mySettings.UI_FLATTEN_PACKAGES)) { path: List<PsiFile>? ->
            if (buf.isNotEmpty()) buf.append("<br>")
            buf.append(path?.joinToString{ psiFile: PsiFile -> psiFile.name }, delim)
            true
        }
        val pane = JEditorPane(UIUtil.HTML_MIME, XmlStringUtil.wrapInHtml(buf))
        pane.setForeground(JBColor.foreground())
        pane.setBackground(HintUtil.getInformationColor())
        pane.setOpaque(true)
        val scrollPane = ScrollPaneFactory.createScrollPane(pane)
        val dimension = pane.getPreferredSize()
        scrollPane.minimumSize = Dimension(dimension.width, dimension.height + 20)
        scrollPane.preferredSize = Dimension(dimension.width, dimension.height + 20)
        JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, pane).setTitle(LangBundle.message("popup.title.dependencies"))
                .setMovable(true).createPopup().showInBestPositionFor(e.dataContext)
    }

    override fun update(e: AnActionEvent) {
        val direct = booleanArrayOf(true)
        processDependencies(getSelectedScope(myLeftTree, mySettings.UI_FLATTEN_PACKAGES), getSelectedScope(myRightTree, mySettings.UI_FLATTEN_PACKAGES)) { path: List<PsiFile>? ->
            direct[0] = false
            false
        }
        e.presentation.setEnabled(!direct[0])
    }

    private fun processDependencies(searchIn: Set<PsiFile>, searchFor: Set<PsiFile>, processor: Processor<in List<PsiFile>>) {
        if (myTransitiveBorder == 0) return
        val initialSearchFor: Set<PsiFile> = HashSet(searchFor)
        for (builder in myBuilders) {
            for (from in searchIn) {
                for (to in initialSearchFor) {
                    val paths = builder.findPaths(from, to)
                    paths.sortWith(Comparator.comparingInt { obj: List<PsiFile?> -> obj.size })
                    for (path in paths) {
                        if (path.isNotEmpty()) {
                            path.add(0, from)
                            path.add(to)
                            if (!processor.process(path)) return
                        }
                    }
                }
            }
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}