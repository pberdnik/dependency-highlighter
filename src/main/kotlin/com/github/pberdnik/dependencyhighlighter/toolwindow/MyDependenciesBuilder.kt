package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.displayUrlRelativeToProject
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packageDependencies.DependenciesBuilder
import com.intellij.packageDependencies.DependencyVisitorFactory
import com.intellij.packageDependencies.DependencyVisitorFactory.VisitorOptions
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiFileEx

/**
 * @author anna
 */
abstract class MyDependenciesBuilder protected constructor(val project: Project, val scope: AnalysisScope) {
    val dependencies: MutableMap<PsiFile, MutableSet<PsiFile>> = HashMap()
    protected var myTotalFileCount: Int = scope.fileCount
    protected var myFileCount = 0

    abstract fun analyze()

    fun findPaths(from: PsiFile, to: PsiFile): List<List<PsiFile>> {
        return findPaths(from, to, HashSet())
    }

    private fun findPaths(from: PsiFile, to: PsiFile, processed: MutableSet<in PsiFile>): List<MutableList<PsiFile>> {
        val result: MutableList<MutableList<PsiFile>> = ArrayList()
        val reachable = dependencies[from]
        if (reachable != null) {
            if (reachable.contains(to)) {
                result.add(ArrayList())
                return result
            }
            if (processed.add(from)) {
                for (file in reachable) {
                    if (!scope.contains(file)) { //exclude paths through scope
                        val paths = findPaths(file, to, processed)
                        for (path in paths) {
                            path.add(0, file)
                        }
                        result.addAll(paths)
                    }
                }
            }
        }
        return result
    }

    fun getRelativeToProjectPath(virtualFile: VirtualFile): String {
        return displayUrlRelativeToProject(virtualFile, virtualFile.presentableUrl, project, isIncludeFilePath = true, moduleOnTheLeft = false)
    }

    companion object {
        @JvmOverloads
        fun analyzeFileDependencies(file: PsiFile,
                                    options: VisitorOptions = VisitorOptions.fromSettings(file.project),
                                    processor: DependenciesBuilder.DependencyProcessor) {
            val prev = file.getUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING)
            file.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, java.lang.Boolean.TRUE)
            try {
                file.accept(DependencyVisitorFactory.createVisitor(file, processor, options))
            } finally {
                file.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, prev)
            }
        }
    }
}
