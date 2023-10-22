package com.github.pberdnik.dependencyhighlighter.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.psi.PsiFile

@Service(Service.Level.PROJECT)
class DependenciesHandlerService {
    val forwardDependencies: MutableMap<PsiFile, Set<PsiFile>> = mutableMapOf()
    val backwardDependencies: MutableMap<PsiFile, MutableSet<PsiFile>> = mutableMapOf()

    fun updateDependencies(dependencyBuilders: MutableList<DependenciesBuilder>) {
        for (builder in dependencyBuilders) {
            forwardDependencies.putAll(builder.dependencies)
        }
        buildBackwardFromForwardDependencies()
    }

    private fun buildBackwardFromForwardDependencies() {
        forwardDependencies.forEach { (psiFile, depsSet) ->
            depsSet.forEach { psiDep ->
                if (backwardDependencies.contains(psiDep)) {
                    backwardDependencies[psiDep]?.add(psiFile)
                } else {
                    val set = hashSetOf(psiFile)
                    backwardDependencies[psiDep] = set
                }
            }
        }
    }
}
