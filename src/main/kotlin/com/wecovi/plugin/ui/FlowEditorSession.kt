package com.wecovi.plugin.ui

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import com.wecovi.plugin.model.FlowDocument
import com.wecovi.plugin.model.FlowNode
import com.wecovi.plugin.model.SourceLocation
import com.wecovi.plugin.service.FlowService

internal class FlowEditorSession(project: Project, private val symbolId: String) {
    private val service = FlowService(project)
    private var rootPointer: SmartPsiElementPointer<JSFunction>? = null
    private var nodes = emptyMap<String, FlowNode>()
    private var targetPointers = emptyMap<String, SmartPsiElementPointer<JSFunction>>()
    var generation = 0
        private set

    fun reload(): FlowDocument {
        val document = service.analyze(rootPointer ?: service.pointer(symbolId) ?: error("Stale flow"))
        rootPointer = rootPointer ?: service.pointer(document.root.symbolId)
        generation += 1
        register(document.nodes, replace = true)
        return document
    }

    fun expand(nodeId: String): FlowDocument {
        val document = service.analyze(targetPointers[nodeId] ?: error("Stale node"))
        register(document.nodes, replace = false)
        return document
    }

    fun source(nodeId: String): SourceLocation = requireNode(nodeId).sourceLocation

    fun isValid() = rootPointer?.element != null

    private fun requireNode(nodeId: String): FlowNode = nodes[nodeId] ?: error("Stale node")

    private fun register(newNodes: List<FlowNode>, replace: Boolean) {
        val flattened = newNodes.flatMap(::flatten)
        nodes = (if (replace) emptyMap() else nodes) + flattened.associateBy(FlowNode::id)
        targetPointers = (if (replace) emptyMap() else targetPointers) + flattened.mapNotNull { node ->
            node.targetSymbolId?.let { target -> service.pointer(target)?.let { node.id to it } }
        }.toMap()
    }

    private fun flatten(node: FlowNode): List<FlowNode> = listOf(node) + node.children.flatMap(::flatten)
}
