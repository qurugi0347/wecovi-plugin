package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.PsiElement
import com.wecovi.plugin.model.SourceLocation

internal fun PsiElement.sourceLocation(path: String) = SourceLocation(
    path = path,
    startOffset = textRange.startOffset,
    endOffset = textRange.endOffset,
)

internal fun JSFunction.signature(): String? {
    val functionName = name ?: return null
    val parameters = parameterVariables.joinToString(", ") { parameter ->
        val parameterName = parameter.name ?: "arg"
        val parameterType = parameter.type?.typeText

        if (parameterType == null) parameterName else "$parameterName: $parameterType"
    }
    val returnType = returnTypeElement?.text ?: "unknown"

    return "$functionName($parameters): $returnType"
}
