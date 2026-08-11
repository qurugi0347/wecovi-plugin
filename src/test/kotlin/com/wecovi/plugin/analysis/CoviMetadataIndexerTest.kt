package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CoviMetadataIndexerTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData"

    fun testGIVENCoviMetadataFunctionsWHENIndexedTHENBuildsSortedFlowAndFunctionEntries() {
        // Given: roots, documented functions, a named arrow function, and an undocumented helper.
        val typeScriptFile = myFixture.configureByFile("typescript/metadata/covi-index.ts")

        // When: the Covi metadata index is built from the TypeScript PSI.
        val index = CoviMetadataIndexer().index(typeScriptFile, typeScriptFile.virtualFile.parent.parent)

        // Then: roots are also functions, groups are split, and undocumented functions are not listed.
        assertEquals(listOf("registerAdmin", "signup"), index.flows.map { it.functionName })
        assertEquals(listOf("validateEmail", "registerAdmin", "signup", "createUser"), index.functions.map { it.functionName })

        val signup = index.flows.single { it.functionName == "signup" }
        assertEquals("사용자 API 가입", signup.title)
        assertEquals(listOf("User API", "User"), signup.groupPath)
        assertEquals("metadata/covi-index.ts#signup", signup.symbolId)
        assertTrue(signup.sourceLocation.startOffset >= 0)
        assertTrue(signup.sourceLocation.endOffset > signup.sourceLocation.startOffset)

        val createUser = index.functions.single { it.functionName == "createUser" }
        assertEquals("사용자를 생성한다", createUser.title)
        assertFalse(createUser.isRoot)
        assertFalse(index.functions.any { it.functionName == "undocumentedHelper" })
    }

    fun testGIVENNamedArrowFunctionWHENOpenedTHENExposesVariableNameToTypeScriptPsi() {
        // Given: a TypeScript fixture with a named arrow declaration.
        val typeScriptFile = myFixture.configureByFile("typescript/metadata/covi-index.ts")

        // When: JavaScript functions are collected from the file PSI.
        val functionNames = PsiTreeUtil.findChildrenOfType(typeScriptFile, JSFunction::class.java).mapNotNull { it.name }

        // Then: the indexer can use the declared variable name as its stable function name.
        assertTrue(functionNames.contains("createUser"))
    }
}
