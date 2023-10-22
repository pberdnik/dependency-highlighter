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

abstract class DependenciesBuilder protected constructor(val project: Project, val scope: AnalysisScope) {
    val dependencies: MutableMap<VirtualFile, MutableSet<VirtualFile>> = HashMap()
    protected var totalFileCount: Int = scope.fileCount
    protected var fileCount = 0

    abstract fun analyze()

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
