package com.wecovi.plugin.ui

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.util.concurrency.AppExecutorUtil
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
                ?.let { item -> item as? FlowTreeItem }
                ?.let { item -> openFlow(project, item.entry) }
        }
        ReadAction.nonBlocking(java.util.concurrent.Callable<List<FlowIndexEntry>> { FlowService(project).listFunctions() })
            .inSmartMode(project)
            .expireWith(toolWindow.disposable)
            .finishOnUiThread(ModalityState.any()) { functions ->
                root.removeAllChildren()
                root.add(category("Flows", functions.filter(FlowIndexEntry::isRoot)))
                root.add(category("Functions", functions))
                (tree.model as DefaultTreeModel).reload()
            }
            .submit(AppExecutorUtil.getAppExecutorService())
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
        parent.add(DefaultMutableTreeNode(FlowTreeItem(entry)))
    }

    private fun openFlow(project: com.intellij.openapi.project.Project, entry: FlowIndexEntry) {
        val manager = FileEditorManager.getInstance(project)
        val file = manager.openFiles.filterIsInstance<FlowVirtualFile>()
            .firstOrNull { it.entry.symbolId == entry.symbolId } ?: FlowVirtualFile(entry)
        manager.openFile(file, true)
    }

    private class FlowTreeItem(val entry: FlowIndexEntry) {
        override fun toString() = entry.title
    }
}
