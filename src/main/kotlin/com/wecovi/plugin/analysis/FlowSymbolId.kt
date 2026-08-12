package com.wecovi.plugin.analysis

import com.intellij.lang.javascript.psi.JSFunction

internal fun flowSymbolId(path: String, function: JSFunction): String =
    "$path#${function.name ?: "unnamed"}@${function.textRange.startOffset}"
