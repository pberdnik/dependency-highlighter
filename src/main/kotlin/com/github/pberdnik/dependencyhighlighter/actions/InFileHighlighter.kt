package com.github.pberdnik.dependencyhighlighter.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.DependenciesBuilder
import com.intellij.analysis.AnalysisScope
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.search.LocalSearchScope

class InFileHighlighter(
        project: Project,
        psiFile: PsiFile,
) : DependenciesBuilder(project, AnalysisScope(LocalSearchScope(psiFile), project)) {
    val deps: MutableSet<PsiElementAndFile> = mutableSetOf()
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
        val virtualFile = file.virtualFile
        val isInLibrary = virtualFile == null || fileIndex.isInLibrary(virtualFile)
        val processed: MutableSet<PsiFile> = HashSet()
        for (psiFile in hashSetOf(file)) {
            val vFile = psiFile.virtualFile
            if (vFile != null) {
                if (!isInLibrary && fileIndex.isInLibrary(vFile)) {
                    processed.add(psiFile)
                }
            }
            if (processed.add(psiFile)) {
                val found: MutableSet<PsiElementAndFile> = HashSet()
                analyzeFileDependencies(psiFile) { place: PsiElement?, dependency: PsiElement ->
                    val dependencyFile = dependency.containingFile
                    if (dependencyFile != null) {
                        if (viewProvider === dependencyFile.viewProvider) return@analyzeFileDependencies
                        val depFile = dependencyFile.virtualFile
                        if (place != null && depFile != null && (fileIndex.isInContent(depFile) || fileIndex.isInLibrary(depFile))) {
                            found.add(PsiElementAndFile(place, dependencyFile))
                        }
                    }
                }
                deps.addAll(found)
                psiManager.dropResolveCaches()
                InjectedLanguageManager.getInstance(file.project).dropFileCaches(psiFile)
            }
        }
    }
}

class PsiElementAndFile(
        val psiElement: PsiElement,
        val psiFile: PsiFile,
)