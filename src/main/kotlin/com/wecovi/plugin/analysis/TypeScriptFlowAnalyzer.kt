package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSExpressionStatement
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.JSNewExpression
import com.intellij.lang.javascript.psi.JSPrefixExpression
import com.intellij.lang.javascript.psi.JSReturnStatement
import com.intellij.lang.javascript.psi.JSStatement
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.psi.PsiElement
import com.wecovi.plugin.model.FlowDocument
import com.wecovi.plugin.model.FlowIndexEntry
import com.wecovi.plugin.model.FlowNode
import com.wecovi.plugin.model.FlowNodeKind

class TypeScriptFlowAnalyzer {
    fun analyze(function: JSFunction, root: FlowIndexEntry): FlowDocument = FlowDocument(
        root = root.copy(signature = root.signature ?: function.signature()),
        nodes = function.block?.statements.orEmpty().flatMap { statement -> nodesForStatement(statement, root, function) },
    )

    private fun nodesForStatement(
        statement: JSStatement,
        root: FlowIndexEntry,
        function: JSFunction,
    ): List<FlowNode> = when (statement) {
        is JSExpressionStatement -> nodesForExpression(statement.expression, root)
        is JSReturnStatement -> nodesForReturn(statement, root, function)
        is JSVarStatement -> statement.variables.flatMap { variable -> nodesForExpression(variable.initializer, root) }
        else -> emptyList()
    }

    private fun nodesForReturn(
        statement: JSReturnStatement,
        root: FlowIndexEntry,
        function: JSFunction,
    ): List<FlowNode> {
        val returnedNodes = nodesForExpression(statement.expression, root)

        return returnedNodes + node(
            element = statement,
            kind = FlowNodeKind.RETURN,
            label = "return",
            codeExpression = statement.text,
            signature = function.returnTypeElement?.text,
            root = root,
        )
    }

    private fun nodesForExpression(expression: JSExpression?, root: FlowIndexEntry): List<FlowNode> = when (expression) {
        null -> emptyList()
        is JSFunction -> emptyList()
        is JSNewExpression -> nodesForCall(expression, FlowNodeKind.CONSTRUCT, root = root)
        is JSCallExpression -> nodesForCall(expression, FlowNodeKind.CALL, root = root)
        is JSPrefixExpression -> nodesForPrefix(expression, root)
        else -> expression.children
            .filterIsInstance<JSExpression>()
            .flatMap { child -> nodesForExpression(child, root) }
    }

    private fun nodesForPrefix(expression: JSPrefixExpression, root: FlowIndexEntry): List<FlowNode> {
        val operand = expression.expression

        return when (operand) {
            is JSNewExpression -> nodesForCall(operand, FlowNodeKind.CONSTRUCT, expression.text, root, expression)
            is JSCallExpression -> nodesForCall(operand, FlowNodeKind.CALL, expression.text, root, expression)
            else -> listOf(
                node(
                    element = expression,
                    kind = FlowNodeKind.AWAIT,
                    label = "await",
                    codeExpression = expression.text,
                    root = root,
                ),
            )
        }
    }

    private fun nodesForCall(
        expression: JSCallExpression,
        kind: FlowNodeKind,
        codeExpression: String = expression.text,
        root: FlowIndexEntry,
        nodeElement: PsiElement = expression,
    ): List<FlowNode> {
        val methodExpression = expression.methodExpression
        val nestedNodes = nodesForExpression(methodExpression, root) +
            expression.argumentList?.arguments.orEmpty().flatMap { argument -> nodesForExpression(argument, root) }
        val label = if (kind == FlowNodeKind.CONSTRUCT) {
            "new ${methodExpression?.text.orEmpty()}"
        } else {
            methodExpression?.text.orEmpty()
        }

        return nestedNodes + node(
            element = nodeElement,
            kind = kind,
            label = label,
            codeExpression = codeExpression,
            root = root,
        )
    }

    private fun node(
        element: PsiElement,
        kind: FlowNodeKind,
        label: String,
        codeExpression: String,
        signature: String? = null,
        root: FlowIndexEntry,
    ) = FlowNode(
        id = "${root.symbolId}:$kind:${element.textRange.startOffset}:${element.textRange.endOffset}",
        kind = kind,
        label = label,
        codeExpression = codeExpression,
        signature = signature,
        sourceLocation = element.sourceLocation(root.sourceLocation.path),
        isDocumented = false,
        expandable = false,
    )

}
