package com.wecovi.plugin.ui

internal fun canvasHtml(bridgeScript: String): String {
    val css = FlowEditor::class.java.getResourceAsStream("/wecovi/ui/flow.css")?.bufferedReader()?.use { it.readText() }
        ?: error("Missing bundled Flow Canvas CSS")
    val script = FlowEditor::class.java.getResourceAsStream("/wecovi/ui/flow.js")?.bufferedReader()?.use { it.readText() }
        ?: error("Missing bundled Flow Canvas JavaScript")
    return """<!doctype html><html><head><style>$css</style></head><body><div id="root"></div><script>$bridgeScript</script><script>$script</script></body></html>"""
}
