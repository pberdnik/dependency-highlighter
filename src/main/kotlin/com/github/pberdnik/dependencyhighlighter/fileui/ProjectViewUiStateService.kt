package com.github.pberdnik.dependencyhighlighter.fileui

import com.github.pberdnik.dependencyhighlighter.Colors
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor

@Service(Service.Level.PROJECT)
class ProjectViewUiStateService(project: Project) {
    private var forwardDeps: Set<VirtualFile> = HashSet()
    private var backwardDeps: Set<VirtualFile> = HashSet()
    private var cycleDeps: Set<VirtualFile> = HashSet()

    val fileColorMap = mutableMapOf<String, JBColor>()
    val forwardDepsNumMap = mutableMapOf<String, Int>()
    val backwardDepsNumMap = mutableMapOf<String, Int>()

    fun getRandomNumber() = (1..100).random()
    fun setDeps(forwardDeps: Set<VirtualFile>, backwardDeps: Set<VirtualFile>, cycleDeps: Set<VirtualFile>) {
        this.forwardDeps = forwardDeps
        this.backwardDeps = backwardDeps
        this.cycleDeps = cycleDeps
        updateUiState()
    }

    private fun updateUiState() {
        fileColorMap.clear()
        forwardDepsNumMap.clear()
        backwardDepsNumMap.clear()
        forwardDeps.forEach { file ->
            fileColorMap.putIfAbsent(file.path, Colors.green)
            var parent = file.parent
            while (parent != null) {
                forwardDepsNumMap[parent.path] = (forwardDepsNumMap[parent.path] ?: 0) + 1
                parent = parent.parent
            }
        }
        backwardDeps.forEach { file ->
            fileColorMap.putIfAbsent(file.path, Colors.purple)
            var parent = file.parent
            while (parent != null) {
                backwardDepsNumMap[parent.path] = (backwardDepsNumMap[parent.path] ?: 0) + 1
                parent = parent.parent
            }
        }
    }

}
