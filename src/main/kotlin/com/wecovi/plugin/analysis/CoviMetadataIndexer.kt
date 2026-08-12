package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.wecovi.plugin.model.FlowIndexEntry
import com.wecovi.plugin.model.SourceLocation

class CoviMetadataIndexer {
    fun index(file: PsiFile, projectRoot: VirtualFile): CoviIndex {
        val projectRelativePath = projectRelativePath(file, projectRoot)
        val entries = PsiTreeUtil.findChildrenOfType(file, JSFunction::class.java)
            .mapNotNull { function -> toIndexEntry(function, projectRelativePath) }
            .sortedWith(indexOrder)

        return CoviIndex(
            flows = entries.filter(FlowIndexEntry::isRoot),
            functions = entries,
        )
    }

    private fun toIndexEntry(function: JSFunction, projectRelativePath: String): FlowIndexEntry? {
        val functionName = function.name ?: return null
        val documentation = documentationFor(function) ?: return null
        val isRoot = documentation.hasTag(ROOT_TAG)
        val coviTag = documentation.firstTag(COVI_TAG)

        if (!isRoot && coviTag == null) {
            return null
        }

        val groupPath = documentation.firstTag(GROUP_TAG)
            ?.getDescriptionText()
            ?.split('/')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
        val title = documentation.description?.getDescriptionText().orEmpty().trim()
            .ifEmpty { coviTag?.getDescriptionText().orEmpty().trim() }
            .ifEmpty { functionName }
        val sourceLocation = SourceLocation(
            path = projectRelativePath,
            startOffset = function.textRange.startOffset,
            endOffset = function.textRange.endOffset,
        )

        return FlowIndexEntry(
            symbolId = flowSymbolId(projectRelativePath, function),
            title = title,
            functionName = functionName,
            groupPath = groupPath,
            sourceLocation = sourceLocation,
            isRoot = isRoot,
        )
    }

    private fun projectRelativePath(file: PsiFile, projectRoot: VirtualFile): String {
        val virtualFile = requireNotNull(file.virtualFile) { "Covi indexing requires a virtual source file." }

        return requireNotNull(VfsUtilCore.getRelativePath(virtualFile, projectRoot, '/')) {
            "Source file must be inside the project base directory."
        }
    }

    private fun documentationFor(function: JSFunction): JSDocComment? =
        generateSequence(function as PsiElement?) { it.parent }
            .take(MAX_DOCUMENTATION_OWNERS)
            .mapNotNull { owner ->
                PsiTreeUtil.getChildOfType(owner, JSDocComment::class.java)
                    ?: PsiTreeUtil.getPrevSiblingOfType(owner, JSDocComment::class.java)
            }
            .firstOrNull()

    private fun JSDocComment.hasTag(name: String) = firstTag(name) != null

    private fun JSDocComment.firstTag(name: String) = tags.firstOrNull { it.name == name }

    private companion object {
        const val ROOT_TAG = "covi-root"
        const val COVI_TAG = "covi"
        const val GROUP_TAG = "covi-group"
        const val MAX_DOCUMENTATION_OWNERS = 2

        val indexOrder = compareBy<FlowIndexEntry>(
            { it.groupPath.joinToString("/") },
            FlowIndexEntry::title,
            FlowIndexEntry::functionName,
        )
    }
}
