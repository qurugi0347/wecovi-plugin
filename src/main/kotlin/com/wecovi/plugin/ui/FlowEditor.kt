package com.wecovi.plugin.ui

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class FlowEditor(private val project: Project, private val file: FlowVirtualFile) : UserDataHolderBase(), FileEditor {
    private val label = JBLabel("Loading flow…")
    private val component = JBPanel<JBPanel<*>>().apply { add(label) }
    private val session = FlowEditorSession(project, file.entry.symbolId)
    private var browser: JBCefBrowser? = null
    private var query: JBCefJSQuery? = null

    init {
        if (JBCefApp.isSupported()) createBrowser() else label.text = "JCEF is unavailable in this IDE."
    }

    private fun createBrowser() {
        val createdBrowser = JBCefBrowser()
        val createdQuery = JBCefJSQuery.create(createdBrowser)
        val bridge = FlowBridge(session, ::sendToCanvas, ::openSource)
        createdQuery.addHandler(bridge::handle)
        browser = createdBrowser
        query = createdQuery
        component.removeAll()
        component.add(createdBrowser.component)
        createdBrowser.loadHTML(canvasHtml("window.wecoviPost = payload => { ${createdQuery.inject("payload")} }"))
    }

    private fun sendToCanvas(json: String) {
        val currentBrowser = browser ?: return
        currentBrowser.cefBrowser.executeJavaScript(
            "window.wecoviReceive(JSON.parse(atob('${base64Json(json)}')))",
            currentBrowser.cefBrowser.url,
            0,
        )
    }

    private fun openSource(nodeId: String) {
        val location = session.source(nodeId)
        ApplicationManager.getApplication().invokeLater {
            val sourceFile = VfsUtilCore.findRelativeFile(location.path, project.baseDir) ?: return@invokeLater
            OpenFileDescriptor(project, sourceFile, location.startOffset).navigate(true)
        }
    }

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent = component
    override fun getName() = file.entry.title
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun setState(state: FileEditorState) = Unit
    override fun isModified() = false
    override fun isValid() = !project.isDisposed && file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun dispose() { query?.dispose(); browser?.dispose() }
}
