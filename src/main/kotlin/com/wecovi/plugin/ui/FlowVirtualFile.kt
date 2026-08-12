package com.wecovi.plugin.ui

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.LightVirtualFile
import com.wecovi.plugin.model.FlowIndexEntry

class FlowVirtualFile(val entry: FlowIndexEntry) : LightVirtualFile(
    "Wecovi: ${entry.title}",
    PlainTextFileType.INSTANCE,
    entry.symbolId,
)
