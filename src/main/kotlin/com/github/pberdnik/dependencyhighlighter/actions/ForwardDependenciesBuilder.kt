package com.github.pberdnik.dependencyhighlighter.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.DependenciesBuilder
import com.intellij.analysis.AnalysisBundle
import com.intellij.analysis.AnalysisScope
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor

class ForwardDependenciesBuilder(project: Project, scope: AnalysisScope) : DependenciesBuilder(project, scope) {
    private val started: MutableSet<VirtualFile?> = HashSet()

    override fun analyze() {
        val psiManager = PsiManager.getInstance(project)
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        psiManager.runInBatchFilesMode {
            scope.acceptIdempotentVisitor(object : PsiRecursiveElementVisitor() {
                override fun visitFile(file: PsiFile) {
                    visit(file, fileIndex, psiManager)
                }
            })
        }
    }

    private fun visit(file: PsiFile, fileIndex: ProjectFileIndex, psiManager: PsiManager) {
        val viewProvider = file.viewProvider
        if (viewProvider.baseLanguage !== file.language) return
        val indicator = ProgressManager.getInstance().progressIndicator
        val virtualFile = file.virtualFile
        if (indicator != null) {
            if (indicator.isCanceled) {
                throw ProcessCanceledException()
            }
            indicator.text = AnalysisBundle.message("package.dependencies.progress.text")
            if (virtualFile != null) {
                indicator.text2 = getRelativeToProjectPath(virtualFile)
            }
            if (totalFileCount > 0 && started.add(virtualFile)) {
                indicator.fraction = (++fileCount).toDouble() / totalFileCount
            }
        }
        val isInLibrary = virtualFile == null || fileIndex.isInLibrary(virtualFile)
        val processed: MutableSet<PsiFile> = HashSet()
        val vFile = file.virtualFile
        if (vFile != null) {
            if (indicator != null) {
                indicator.text2 = getRelativeToProjectPath(vFile)
            }
            if (!isInLibrary && fileIndex.isInLibrary(vFile)) {
                processed.add(file)
            }
        }
        if (processed.add(file)) {
            val found: MutableSet<PsiFile> = HashSet()
            analyzeFileDependencies(file) { _: PsiElement?, dependency: PsiElement ->
                val dependencyFile = dependency.containingFile
                if (dependencyFile != null) {
                    if (viewProvider === dependencyFile.viewProvider) return@analyzeFileDependencies
                    val depFile = dependencyFile.virtualFile
                    if (depFile != null && (fileIndex.isInContent(depFile) || fileIndex.isInLibrary(depFile))) {
                        val navigationElement = dependencyFile.navigationElement
                        found.add(if (navigationElement is PsiFile) navigationElement else dependencyFile)
                    }
                }
            }
            val deps = dependencies.computeIfAbsent(file) { _: PsiFile? -> HashSet() }
            deps.addAll(found)
            psiManager.dropResolveCaches()
            InjectedLanguageManager.getInstance(file.project).dropFileCaches(file)
        }
    }
}
