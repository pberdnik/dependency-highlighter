package com.github.pberdnik.dependencyhighlighter

import com.github.pberdnik.dependencyhighlighter.toolwindow.DependenciesHandlerService
import com.github.pberdnik.dependencyhighlighter.toolwindow.PluginSetting
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class FilesChangeListener(
    private val project: Project,
) : BulkFileListener {
    private val dependenciesHandlerService = project.service<DependenciesHandlerService>()
    private val dependenciesHandler = project.service<DependenciesHandlerService>()
    private val settings = project.service<PluginSetting>()

    override fun after(events: MutableList<out VFileEvent>) {
        events.forEach { action ->
            dependenciesHandlerService.fileChanged(action.file)
        }
        if (settings.autoReanalyse) {
            val progressTitle = CodeInsightBundle.message("package.dependencies.progress.title")
            runBackgroundableTask(progressTitle, project, true) { indicator ->
                indicator.isIndeterminate = false
                dependenciesHandler.reanalyseChanged()
            }
        }
    }
}