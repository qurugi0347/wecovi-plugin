package com.wecovi.plugin.ui

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class FlowEditorProvider : FileEditorProvider {
    override fun accept(project: Project, file: VirtualFile) = file is FlowVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        FlowEditor(project, file as FlowVirtualFile)

    override fun getEditorTypeId() = "wecovi.flow"

    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
