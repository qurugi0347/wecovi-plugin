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
        val file = myFixture.configureByFiles(
            "typescript/call-boundary/basic.ts",
            "typescript/call-boundary/excluded.test.ts",
        ).first()
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
        assertEquals(
            "call-boundary/basic.ts#documentedTarget@${functionNamed(file, "documentedTarget").textRange.startOffset}",
            documented.targetSymbolId,
        )
        assertEquals("documentedTarget(): void", documented.signature)

        val plain = resolvedNodes.single { it.label == "plainTarget" }
        assertTrue(plain.expandable)
        assertFalse(plain.isDocumented)
        assertNotNull(plain.targetSymbolId)
        assertEquals("plainTarget(): void", plain.signature)

        val construct = resolvedNodes.single { it.label == "new constructTarget" }
        assertTrue(construct.expandable)
        assertEquals("constructTarget(value: string): void", construct.signature)

        val excluded = resolvedNodes.single { it.label == "excludedTarget" }
        assertEquals(BoundaryKind.EXTERNAL, excluded.boundaryKind)
        assertFalse(excluded.expandable)

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
