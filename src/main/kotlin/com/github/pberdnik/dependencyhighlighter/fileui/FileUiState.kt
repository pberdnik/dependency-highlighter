package com.github.pberdnik.dependencyhighlighter.fileui

import com.intellij.ui.JBColor

data class FileUiState(
        val color: JBColor?,
        val numForward: Int = 0,
        val numBackward: Int = 0,
        val numCycle: Int = 0,
)