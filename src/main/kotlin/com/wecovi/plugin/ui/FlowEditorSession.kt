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
    var generation = 0
        private set

    fun reload(): FlowDocument {
        val document = service.analyze(rootPointer ?: service.pointer(symbolId) ?: error("Stale flow"))
        rootPointer = service.pointer(document.root.symbolId)
        generation += 1
        nodes = document.nodes.associateBy(FlowNode::id)
        return document
    }

    fun expand(nodeId: String): FlowDocument {
        val target = requireNode(nodeId).targetSymbolId ?: error("Node cannot be expanded")
        return service.analyze(service.pointer(target) ?: error("Stale node"))
    }

    fun source(nodeId: String): SourceLocation = requireNode(nodeId).sourceLocation

    fun isValid() = rootPointer?.element != null

    private fun requireNode(nodeId: String): FlowNode = nodes[nodeId] ?: error("Stale node")
}
