// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies.actions

import com.github.pberdnik.dependencyhighlighter.panel.FileAnalyzeDependenciesHandler
import com.intellij.analysis.AnalysisScope
import com.intellij.analysis.BaseAnalysisAction
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.project.Project

class MyAnalyzeDependenciesAction : BaseAnalysisAction(CodeInsightBundle.messagePointer("action.forward.dependency.analysis"), CodeInsightBundle.messagePointer("action.analysis.noun")) {
    override fun analyze(project: Project, scope: AnalysisScope) {
        FileAnalyzeDependenciesHandler(project, listOf(scope), 0).analyze()
    }
}
