package com.wecovi.plugin.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class FlowServiceTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData"

    fun testGIVENRootAndInternalTargetsWHENAnalyzedAndExpandedTHENReturnsOnlyRequestedBodies() {
        val file = myFixture.configureByFile("typescript/service/flow.ts")
        val service = FlowService(project, file.virtualFile.parent.parent)

        val root = service.listFlows().single()
        val document = service.analyze(root.symbolId)
        val documented = document.nodes.single { it.label == "documented" }

        assertTrue(documented.expandable)
        assertTrue(documented.children.isEmpty())
        assertEquals("documented", service.expand(requireNotNull(documented.targetSymbolId)).root.functionName)
        assertEquals(listOf("root"), service.listFlows().map { it.functionName })
        assertEquals(listOf("documented", "root"), service.listFunctions().map { it.functionName })
        assertThrows(IllegalStateException::class.java) { service.analyze("missing") }
    }
}
