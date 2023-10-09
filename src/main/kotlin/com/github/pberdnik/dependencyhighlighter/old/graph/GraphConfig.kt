package com.github.pberdnik.dependencyhighlighter.old.graph

import com.github.pberdnik.dependencyhighlighter.storage.GraphConfigStorageService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir

class GraphConfig(
    val project: Project
) {
    val projectDir: String = project.guessProjectDir()?.path ?: ""

    private val graphConfigState = project.service<GraphConfigStorageService>().state

    var filteredModules = mutableSetOf<String>()
    var filteredClasses = mutableSetOf<String>()

    val greenModules: MutableSet<String> get() = graphConfigState.greenModules
    var redClasses = mutableSetOf<String>()
}