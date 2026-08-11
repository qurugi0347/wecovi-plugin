package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.wecovi.plugin.model.BoundaryKind
import com.wecovi.plugin.model.FlowIndexEntry
import com.wecovi.plugin.model.SourceLocation

class CallTargetResolverTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData"

    fun testGIVENResolvedAndUnresolvedCallsWHENResolvedTHENOnlyProjectTargetsAreExpandable() {
        val file = myFixture.configureByFile("typescript/call-boundary/basic.ts")
        val root = functionNamed(file, "root")
        val rootEntry = FlowIndexEntry(
            symbolId = "call-boundary/basic.ts#root@${root.textRange.startOffset}",
            title = "root",
            functionName = "root",
            groupPath = emptyList(),
            sourceLocation = SourceLocation("call-boundary/basic.ts", root.textRange.startOffset, root.textRange.endOffset),
            isRoot = true,
        )
        val nodes = TypeScriptFlowAnalyzer().analyze(root, rootEntry).nodes
        val resolvedNodes = nodes.map { node -> CallTargetResolver().resolve(node, file, file.virtualFile.parent.parent) }

        val documented = resolvedNodes.single { it.label == "documentedTarget" }
        assertTrue(documented.expandable)
        assertTrue(documented.isDocumented)
        assertEquals("call-boundary/basic.ts#documentedTarget@0", documented.targetSymbolId)

        val plain = resolvedNodes.single { it.label == "plainTarget" }
        assertTrue(plain.expandable)
        assertFalse(plain.isDocumented)
        assertNotNull(plain.targetSymbolId)

        val external = resolvedNodes.single { it.label == "Array.isArray" }
        assertEquals(BoundaryKind.EXTERNAL, external.boundaryKind)
        assertFalse(external.expandable)

        val unresolved = resolvedNodes.single { it.label == "unknownTarget" }
        assertEquals(BoundaryKind.UNRESOLVED, unresolved.boundaryKind)
        assertFalse(unresolved.expandable)
        assertNull(unresolved.targetSymbolId)
    }

    private fun functionNamed(file: PsiFile, name: String): JSFunction =
        PsiTreeUtil.findChildrenOfType(file, JSFunction::class.java).single { it.name == name }
}
