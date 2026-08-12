package com.wecovi.plugin.service

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import com.wecovi.plugin.analysis.CallTargetResolver
import com.wecovi.plugin.analysis.CoviMetadataIndexer
import com.wecovi.plugin.analysis.TypeScriptFlowAnalyzer
import com.wecovi.plugin.analysis.flowSymbolId
import com.wecovi.plugin.analysis.isAnalysisSource
import com.wecovi.plugin.analysis.sourceLocation
import com.wecovi.plugin.analysis.signature
import com.wecovi.plugin.model.FlowDocument
import com.wecovi.plugin.model.FlowIndexEntry

class FlowService(
    private val project: Project,
    private val projectRoot: VirtualFile = requireNotNull(project.baseDir),
) {
    fun listFlows(): List<FlowIndexEntry> = listEntries().filter(FlowIndexEntry::isRoot)

    fun listFunctions(): List<FlowIndexEntry> = listEntries()

    fun analyze(symbolId: String): FlowDocument = analyze(findFunction(symbolId) ?: error("Unknown flow: $symbolId"))

    fun analyze(pointer: SmartPsiElementPointer<JSFunction>): FlowDocument =
        analyze(pointer.element ?: error("Stale flow"))

    fun expand(symbolId: String): FlowDocument = analyze(findFunction(symbolId) ?: error("Unknown flow: $symbolId"))

    fun pointer(symbolId: String): SmartPsiElementPointer<JSFunction>? =
        findFunction(symbolId)?.let { function -> SmartPointerManager.getInstance(project).createSmartPsiElementPointer(function) }

    private fun listEntries(): List<FlowIndexEntry> = sourceFiles()
        .flatMap { file -> CoviMetadataIndexer().index(file, projectRoot).functions }
        .sortedWith(compareBy({ it.groupPath.joinToString("/") }, FlowIndexEntry::title, FlowIndexEntry::functionName))

    private fun findFunction(symbolId: String): JSFunction? = sourceFiles()
        .asSequence()
        .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, JSFunction::class.java).asSequence() }
        .firstOrNull { function -> flowSymbolId(pathOf(function), function) == symbolId }

    fun analyze(function: JSFunction): FlowDocument {
        val path = pathOf(function)
        val root = listEntries().firstOrNull { it.symbolId == flowSymbolId(path, function) }
            ?: FlowIndexEntry(
                symbolId = flowSymbolId(path, function),
                title = function.name ?: "Unnamed function",
                functionName = function.name ?: "unnamed",
                groupPath = emptyList(),
                signature = function.signature(),
                sourceLocation = function.sourceLocation(path),
                isRoot = false,
            )
        val file = requireNotNull(function.containingFile)

        return TypeScriptFlowAnalyzer().analyze(function, root).let { document ->
            document.copy(nodes = document.nodes.map { node -> CallTargetResolver().resolve(node, file, projectRoot) })
        }
    }

    private fun sourceFiles() = buildList {
        VfsUtilCore.iterateChildrenRecursively(projectRoot, null) { virtualFile ->
            if (isAnalysisSource(project, virtualFile)) {
                PsiManager.getInstance(project).findFile(virtualFile)?.let(::add)
            }
            true
        }
    }

    private fun pathOf(function: JSFunction): String = requireNotNull(
        VfsUtilCore.getRelativePath(requireNotNull(function.containingFile.virtualFile), projectRoot, '/'),
    )
}
