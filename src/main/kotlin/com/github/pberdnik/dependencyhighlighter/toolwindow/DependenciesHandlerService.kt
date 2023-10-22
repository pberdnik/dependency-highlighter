package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

@Service(Service.Level.PROJECT)
class DependenciesHandlerService(
    private val project: Project,
) {
    val myDependencies: MutableMap<PsiFile, Set<PsiFile>> = mutableMapOf()
    val myBackwardDependencies: MutableMap<PsiFile, MutableSet<PsiFile>> = mutableMapOf()

    fun updateDependencies(dependencyBuilders: MutableList<MyDependenciesBuilder>,) {
        for (builder in dependencyBuilders) {
            myDependencies.putAll(builder.dependencies)
        }
        buildBackwardFromForwardDependencies()
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
}
