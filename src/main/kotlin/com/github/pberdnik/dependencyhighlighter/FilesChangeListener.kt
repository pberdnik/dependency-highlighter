package com.github.pberdnik.dependencyhighlighter

import com.github.pberdnik.dependencyhighlighter.toolwindow.DependenciesHandlerService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*

class FilesChangeListener(
    project: Project,
) : BulkFileListener {
    private var eventNumAfter = 1

    private val dependenciesHandlerService = project.service<DependenciesHandlerService>()

    override fun after(events: MutableList<out VFileEvent>) {
        thisLogger().warn("EVENT $eventNumAfter: $events")
        events.forEach { action ->
            when (action) {
                is VFileContentChangeEvent -> {
                    thisLogger().warn("    CONTENT CHANGE: ${action.path} isDir=${action.file.isDirectory}")
                }

                is VFilePropertyChangeEvent -> {
                    thisLogger().warn("    PATH CHANGE: ${action.oldPath} ${action.newPath} isDir=${action.file.isDirectory}")
                }

                is VFileDeleteEvent -> {
                    thisLogger().warn("    DELETE: ${action.path} isDir=${action.file.isDirectory}")
                }

                is VFileCreateEvent -> {
                    thisLogger().warn("    CREATE: ${action.path} isDir=${action.file?.isDirectory}")
                }

                is VFileCopyEvent -> {
                    thisLogger().warn("    COPY: ${action.path} isDir=${action.file.isDirectory}")
                }

                is VFileMoveEvent -> {
                    thisLogger().warn("    MOVE: ${action.path} isDir=${action.file.isDirectory}")
                }

                else -> {
                    thisLogger().warn("    EVENT $eventNumAfter ${action.javaClass}: ${action.file}")
                }
            }
        }
        eventNumAfter++
    }
}