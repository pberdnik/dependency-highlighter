// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.pberdnik.dependencyhighlighter.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.MyDependenciesBuilder
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
import com.intellij.psi.search.GlobalSearchScope

class MyForwardDependenciesBuilder(project: Project, scope: AnalysisScope, transitive: Int) : MyDependenciesBuilder(project, scope) {
    private val transitiveBorder: Int = transitive
    private val myTargetScope: GlobalSearchScope?
    private val myStarted: MutableSet<VirtualFile?> = HashSet()

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
            if (myTotalFileCount > 0 && myStarted.add(virtualFile)) {
                indicator.fraction = (++myFileCount).toDouble() / myTotalFileCount
            }
        }
        val isInLibrary = virtualFile == null || fileIndex.isInLibrary(virtualFile)
        val collectedDeps: MutableSet<PsiFile> = HashSet()
        collectedDeps.add(file)
        var depth = 0
        val processed: MutableSet<PsiFile> = HashSet()
        do {
            if (depth++ > transitiveBorder) return
            for (psiFile in HashSet<PsiFile>(collectedDeps)) {
                val vFile = psiFile.virtualFile
                if (vFile != null) {
                    if (indicator != null) {
                        indicator.text2 = getRelativeToProjectPath(vFile)
                    }
                    if (!isInLibrary && fileIndex.isInLibrary(vFile)) {
                        processed.add(psiFile)
                    }
                }
                if (processed.add(psiFile)) {
                    val found: MutableSet<PsiFile> = HashSet()
                    analyzeFileDependencies(psiFile) { _: PsiElement?, dependency: PsiElement ->
                        val dependencyFile = dependency.containingFile
                        if (dependencyFile != null) {
                            if (viewProvider === dependencyFile.viewProvider) return@analyzeFileDependencies
//                            if (!dependencyFile.isPhysical) {
//                                thisLogger().warn("NOT PHYSICAL: \n|    " +
//                                        "file = $psiFile \n|    " +
//                                        "dependencyFile = $dependencyFile \n|    " +
//                                        "virtualFile = ${dependencyFile.virtualFile} \n |    " +
//                                        "path = ${dependencyFile.virtualFile?.path}")
//                                if (dependencyFile.virtualFile?.path.isNullOrEmpty()) {
//                                    thisLogger().warn("====================EMPTY====================")
//                                }
//                            }
                            val depFile = dependencyFile.virtualFile
                            if (depFile != null && (fileIndex.isInContent(depFile) || fileIndex.isInLibrary(depFile))
                                    && (myTargetScope == null || myTargetScope.contains(depFile))) {
                                val navigationElement = dependencyFile.navigationElement
                                found.add(if (navigationElement is PsiFile) navigationElement else dependencyFile)
                            }
                        }
                    }
                    val deps = dependencies.computeIfAbsent(file) { _: PsiFile? -> HashSet() }
                    deps.addAll(found)
                    collectedDeps.addAll(found)
                    psiManager.dropResolveCaches()
                    InjectedLanguageManager.getInstance(file.project).dropFileCaches(psiFile)
                }
            }
            collectedDeps.removeAll(processed)
        } while (isTransitive && collectedDeps.isNotEmpty())
    }

    private val isTransitive: Boolean
        get() = transitiveBorder > 0

    init {
        myTargetScope = null
    }
}
