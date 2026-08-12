package com.wecovi.plugin.analysis

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

internal fun isAnalysisSource(project: Project, file: VirtualFile): Boolean =
    ProjectFileIndex.getInstance(project).isInContent(file) &&
        file.extension in setOf("ts", "tsx") &&
        !file.name.endsWith(".d.ts") &&
        !TEST_OR_SPEC_FILE.matches(file.name) &&
        generateSequence(file.parent) { it.parent }.none { parent -> parent.name in EXCLUDED_DIRECTORIES }

private val TEST_OR_SPEC_FILE = Regex(".*\\.(test|spec)\\.[^.]+$")
private val EXCLUDED_DIRECTORIES = setOf("node_modules", "dist", "build", "generated", "__tests__")
