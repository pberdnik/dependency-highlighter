package com.github.pberdnik.dependencyhighlighter.utils

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import javax.swing.SwingUtilities

object UIUtils {

    private var lastInvokeTimeMs: Long = 0
    private var lastUpdateTimeMs: Long = 0
    private const val UPDATE_THRESHOLD = 100
    fun updateUI(project: Project?) {
        debounce {
            updateProjectView(project)
            updateOpenTabs(project)
        }
    }

    private fun updateOpenTabs(project: Project?) {
        if (project == null) return
        val editorManagerEx = FileEditorManagerEx.getInstanceEx(project)
        val openFiles = editorManagerEx.openFiles
        for (openFile in openFiles) {
            editorManagerEx.updateFilePresentation(openFile!!)
        }
    }

    private fun updateProjectView(project: Project?) {
        if (project == null) return
        val projectView = ProjectView.getInstance(project)
        val projectViewPane = projectView.currentProjectViewPane
        projectViewPane?.updateFromRoot(true)
    }

    private fun debounce(task: Runnable) {
        lastInvokeTimeMs = System.currentTimeMillis()
        SwingUtilities.invokeLater {
            val currentTimeMs = System.currentTimeMillis()
            val invokeExpirationTimeMs = lastInvokeTimeMs + UPDATE_THRESHOLD - 10
            val updateExpirationTimeMs = lastUpdateTimeMs + UPDATE_THRESHOLD
            if (currentTimeMs >= invokeExpirationTimeMs || currentTimeMs >= updateExpirationTimeMs) {
                task.run()
                lastUpdateTimeMs = currentTimeMs
            }
        }
    }
}
