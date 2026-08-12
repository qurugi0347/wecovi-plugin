package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSNewExpression
import com.intellij.lang.javascript.psi.JSPsiReferenceElement
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.wecovi.plugin.model.BoundaryKind
import com.wecovi.plugin.model.FlowNode
import com.wecovi.plugin.model.FlowNodeKind

class CallTargetResolver {
    fun resolve(node: FlowNode, file: PsiFile, projectRoot: VirtualFile): FlowNode {
        if (node.kind !in setOf(FlowNodeKind.CALL, FlowNodeKind.CONSTRUCT)) return node

        val call = PsiTreeUtil.findChildrenOfType(file, JSCallExpression::class.java)
            .filter { call ->
                call.textRange.startOffset >= node.sourceLocation.startOffset &&
                    call.textRange.endOffset <= node.sourceLocation.endOffset
            }
            .maxByOrNull { call -> call.textRange.length }
            ?: return unresolved(node)
        val target = (call.methodExpression as? JSPsiReferenceElement)?.resolve() ?: return unresolved(node)
        val targetFile = target.containingFile?.virtualFile ?: return unresolved(node)

        if (!isProjectContent(targetFile, file)) {
            return node.copy(
                targetSymbolId = null,
                boundaryKind = BoundaryKind.EXTERNAL,
                isDocumented = false,
                expandable = false,
            )
        }

        val function = targetFunction(target) ?: return unresolved(node)
        val path = VfsUtilCore.getRelativePath(targetFile, projectRoot, '/') ?: return unresolved(node)
        val symbolId = flowSymbolId(path, function)
        val isDocumented = CoviMetadataIndexer().index(function.containingFile, projectRoot).functions
            .any { entry -> entry.symbolId == symbolId }

        return node.copy(
            targetSymbolId = symbolId,
            boundaryKind = null,
            isDocumented = isDocumented,
            expandable = true,
            signature = function.signature(),
        )
    }

    private fun targetFunction(target: com.intellij.psi.PsiElement): JSFunction? = when (target) {
        is JSFunction -> target
        is JSVariable -> PsiTreeUtil.getChildOfType(target, JSFunction::class.java)
        else -> null
    }

    private fun unresolved(node: FlowNode) = node.copy(
        targetSymbolId = null,
        boundaryKind = BoundaryKind.UNRESOLVED,
        isDocumented = false,
        expandable = false,
    )

    private fun isProjectContent(targetFile: VirtualFile, file: PsiFile) = isAnalysisSource(file.project, targetFile)
}
