package com.github.pberdnik.dependencyhighlighter

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*

class SyncFilesChangeListener : BulkFileListener {
    private var eventNumAfter = 1

    override fun after(events: MutableList<out VFileEvent>) {
        thisLogger().warn("EVENT $eventNumAfter: $events")
        events.forEach { action ->
            when (action) {
                is VFileContentChangeEvent -> {
                    thisLogger().warn("    CONTENT CHANGE: ${action.path} isDir=${action.file.isDirectory}")
                }

                is VFilePropertyChangeEvent -> {
                    thisLogger().warn("    PATH CHANGE: ${action.oldPath} ${action.newPath} isDir=${action.file?.isDirectory}")
                }

                is VFileDeleteEvent -> {
                    thisLogger().warn("    DELETE: ${action.path} isDir=${action.file?.isDirectory}")
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