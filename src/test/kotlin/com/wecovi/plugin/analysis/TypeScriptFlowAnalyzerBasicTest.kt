package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.wecovi.plugin.model.FlowIndexEntry
import com.wecovi.plugin.model.FlowNodeKind
import com.wecovi.plugin.model.SourceLocation

class TypeScriptFlowAnalyzerBasicTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData"

    fun testGIVENBasicStatementsWHENAnalyzedTHENPreservesEvaluationOrderAndRootSignature() {
        val file = myFixture.configureByFile("typescript/basic-flow/analyzer.ts")
        val function = functionNamed(file, "syncFlow")
        val document = TypeScriptFlowAnalyzer().analyze(function, rootFor(function, "basic-flow/analyzer.ts"))

        assertEquals("syncFlow(): string", document.root.signature)
        assertEquals(
            listOf("loadDto()", "new User(dto)", "save(user)", "return save(user);"),
            document.nodes.map { it.codeExpression },
        )
        assertEquals(
            listOf(FlowNodeKind.CALL, FlowNodeKind.CONSTRUCT, FlowNodeKind.CALL, FlowNodeKind.RETURN),
            document.nodes.map { it.kind },
        )
        assertTrue(document.nodes.all { it.id.startsWith("basic-flow/analyzer.ts#syncFlow@") })
    }

    fun testGIVENAwaitAndNestedExpressionsWHENAnalyzedTHENKeepsPostOrderAndSkipsUnsupportedSubtrees() {
        val file = myFixture.configureByFile("typescript/basic-flow/analyzer.ts")
        val analyzer = TypeScriptFlowAnalyzer()

        val asyncDocument = analyzer.analyze(functionNamed(file, "asyncFlow"), rootFor(functionNamed(file, "asyncFlow"), "basic-flow/analyzer.ts"))
        assertEquals(listOf("loadDto()", "new User(loadDto())", "await save(new User(loadDto()))", "await ready"), asyncDocument.nodes.map { it.codeExpression })
        assertEquals(listOf(FlowNodeKind.CALL, FlowNodeKind.CONSTRUCT, FlowNodeKind.CALL, FlowNodeKind.AWAIT), asyncDocument.nodes.map { it.kind })

        val nestedFunction = functionNamed(file, "nestedFlow")
        val nestedDocument = analyzer.analyze(nestedFunction, rootFor(nestedFunction, "basic-flow/analyzer.ts"))
        assertEquals(listOf("inner()", "outer(inner())", "loadDto()", "new User(loadDto())", "[\"callback\"].map(value => save(new User(value)))"), nestedDocument.nodes.map { it.codeExpression })
        assertFalse(nestedDocument.nodes.any { it.codeExpression.contains("conditional") })
        assertFalse(nestedDocument.nodes.any { it.codeExpression == "save(new User(value))" })
    }

    private fun functionNamed(file: com.intellij.psi.PsiFile, name: String): JSFunction =
        PsiTreeUtil.findChildrenOfType(file, JSFunction::class.java).single { it.name == name }

    private fun rootFor(function: JSFunction, path: String) = FlowIndexEntry(
        symbolId = "$path#${function.name}@${function.textRange.startOffset}",
        title = function.name.orEmpty(),
        functionName = function.name.orEmpty(),
        groupPath = emptyList(),
        sourceLocation = SourceLocation(path, function.textRange.startOffset, function.textRange.endOffset),
        isRoot = true,
    )
}
