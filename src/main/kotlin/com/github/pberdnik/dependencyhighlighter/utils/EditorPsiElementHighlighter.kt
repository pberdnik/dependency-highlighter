package com.github.pberdnik.dependencyhighlighter.utils

import com.github.pberdnik.dependencyhighlighter.Colors
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import kotlin.math.min

internal class EditorPsiElementHighlighter(private val _project: Project) {
    private val _textAttributes: TextAttributes = TextAttributes().apply {
        backgroundColor = Colors.green
    }
    private var highlighters: MutableList<RangeHighlighter> = mutableListOf()
    private var _editor: Editor? = null

    fun highlightElement(psiElement: PsiElement) {
        ApplicationManager.getApplication().runReadAction { apply(psiElement) }
    }

    fun removeHighlight() {
        ApplicationManager.getApplication().runReadAction { this.clear() }
    }

    private fun apply(element: PsiElement) {
        _editor = FileEditorManager.getInstance(_project).selectedTextEditor
        val editor = _editor ?: return
        val textRange = element.textRange
        debug("Adding highlighting for $textRange")
        val docTextLength = editor.document.textLength
        highlighters.add(editor.markupModel.addRangeHighlighter(
                textRange.startOffset,
                min(textRange.endOffset, docTextLength),
                HIGHLIGHT_LAYER,
                _textAttributes,
                HighlighterTargetArea.EXACT_RANGE))
    }

    private fun clear() {
        highlighters.forEach { highlighter ->
            if (highlighter.isValid) {
                debug("Removing highlighter for $highlighter")
                _editor?.markupModel?.removeHighlighter(highlighter)
            }
        }
        highlighters.clear()
    }

    companion object {
        private fun debug(message: String) {
            thisLogger().warn(message)
        }
    }
}

private const val HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 100