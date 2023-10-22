package com.github.pberdnik.dependencyhighlighter.toolwindow.actions

import com.github.pberdnik.dependencyhighlighter.toolwindow.PluginSetting
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.PlatformIcons

class ChooseBaseDirAction(
    private val project: Project,
    private val rebuild: () -> Unit,
) : AnAction(
    "Choose Base Directory", "Choose base directory",
    PlatformIcons.PROJECT_ICON) {
    override fun actionPerformed(e: AnActionEvent) {
        val settings = project.service<PluginSetting>()
        settings.baseDir = FileChooser.chooseFile(
            FileChooserDescriptor(
                 false, // chooseFiles
                 true, // chooseFolders
                 false, // chooseJars
                 false, // chooseJarsAsFiles
                 false, // chooseJarContents
                 false, // chooseMultiple
            ),
            project,
            project.guessProjectDir())?.path
        rebuild()
    }
}