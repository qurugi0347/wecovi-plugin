package com.wecovi.plugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.wecovi.plugin.model.FlowIndexEntry
import com.wecovi.plugin.service.FlowService
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CoviToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: com.intellij.openapi.project.Project, toolWindow: ToolWindow) {
        val root = DefaultMutableTreeNode("Covi")
        val tree = Tree(DefaultTreeModel(root))
        val content = toolWindow.contentManager.factory.createContent(JBScrollPane(tree), null, false)
        toolWindow.contentManager.addContent(content)

        tree.addTreeSelectionListener {
            (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject
                ?.let { entry -> entry as? FlowIndexEntry }
                ?.let { entry -> FileEditorManager.getInstance(project).openFile(FlowVirtualFile(entry), true) }
        }
        ApplicationManager.getApplication().runReadAction {
            val service = FlowService(project)
            root.add(category("Flows", service.listFlows()))
            root.add(category("Functions", service.listFunctions()))
            (tree.model as DefaultTreeModel).reload()
        }
    }

    private fun category(name: String, entries: List<FlowIndexEntry>) = DefaultMutableTreeNode(name).apply {
        entries.forEach { entry -> addEntry(entry) }
    }

    private fun DefaultMutableTreeNode.addEntry(entry: FlowIndexEntry) {
        var parent = this
        (if (entry.groupPath.isEmpty()) listOf("Ungrouped") else entry.groupPath).forEach { group ->
            parent = (0 until parent.childCount)
                .map(parent::getChildAt)
                .filterIsInstance<DefaultMutableTreeNode>()
                .firstOrNull { it.userObject == group }
                ?: DefaultMutableTreeNode(group).also(parent::add)
        }
        parent.add(DefaultMutableTreeNode(entry))
    }
}
