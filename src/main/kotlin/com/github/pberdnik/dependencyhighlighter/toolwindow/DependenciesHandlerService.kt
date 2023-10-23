package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.github.pberdnik.dependencyhighlighter.actions.ForwardDependenciesBuilder
import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class DependenciesHandlerService(
    private val project: Project,
) {
    private val forwardDependencies: MutableMap<VirtualFile, Set<VirtualFile>> = hashMapOf()
    private val backwardDependencies: MutableMap<VirtualFile, MutableSet<VirtualFile>> = hashMapOf()

    private val changedFiles: MutableSet<VirtualFile> = hashSetOf()

    fun updateDependencies(dependencyBuilders: MutableList<DependenciesBuilder>) {
        for (builder in dependencyBuilders) {
            forwardDependencies.putAll(builder.dependencies)
        }
        buildBackwardFromForwardDependencies()
    }

    private fun buildBackwardFromForwardDependencies() {
        forwardDependencies.forEach { (file, depsSet) ->
            depsSet.forEach { fileDep ->
                if (backwardDependencies.contains(fileDep)) {
                    backwardDependencies[fileDep]?.add(file)
                } else {
                    val set = hashSetOf(file)
                    backwardDependencies[fileDep] = set
                }
            }
        }
    }

    fun fileChanged(file: VirtualFile?) {
        if (file == null) return
        if (file.isDirectory) return
        changedFiles.add(file)
    }

    fun reanalyseChanged() {
        val depBuilder = ForwardDependenciesBuilder(project, AnalysisScope(project, changedFiles))
        depBuilder.analyze()
        val deps = depBuilder.dependencies
        deps.forEach { (file, newDeps) ->
            val oldDeps = forwardDependencies[file]
            oldDeps?.forEach { oldDep ->
                if (!newDeps.contains(oldDep)) {
                    backwardDependencies[oldDep]?.remove(file)
                }
            }
            newDeps.forEach { newDep ->
                if (backwardDependencies.contains(newDep)) {
                    backwardDependencies[newDep]?.add(file)
                } else {
                    val set = hashSetOf(file)
                    backwardDependencies[newDep] = set
                }
            }

            forwardDependencies[file] = newDeps
        }
        changedFiles.clear()
    }

    fun getForwardDependencies(virtualFile: VirtualFile?): Set<VirtualFile> {
        if (virtualFile == null) return setOf()
        return forwardDependencies[virtualFile] ?: setOf()
    }

    fun getBackwardDependencies(virtualFile: VirtualFile?): Set<VirtualFile> {
        if (virtualFile == null) return setOf()
        return backwardDependencies[virtualFile] ?: setOf()
    }
}
