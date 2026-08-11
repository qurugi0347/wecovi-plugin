package com.wecovi.plugin

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TypeScriptPsiSmokeTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/testData"

    fun testGIVENValidTypeScriptFixtureWHENOpenedTHENRecognizesSignupFunction() {
        // Given: a fixture with TypeScript-specific syntax.
        val fixturePath = "typescript/smoke/basic.ts"

        // When: the platform fixture opens the TypeScript source file.
        val typeScriptFile = myFixture.configureByFile(fixturePath)

        // Then: the file parses as TypeScript without errors and exposes signup once.
        assertEquals("TypeScript", typeScriptFile.language.id)
        assertNull(PsiTreeUtil.findChildOfType(typeScriptFile, PsiErrorElement::class.java))

        val signupFunctions = PsiTreeUtil.findChildrenOfType(typeScriptFile, JSFunction::class.java)
            .filter { it.name == "signup" }

        assertEquals(1, signupFunctions.size)
    }
}
