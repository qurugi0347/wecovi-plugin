package com.wecovi.plugin.ui

private object FlowCanvasResources

internal fun canvasHtml(bridgeScript: String): String {
    val loader = FlowCanvasResources::class.java.classLoader
    val css = loader.getResourceAsStream("wecovi/ui/flow.css")?.bufferedReader()?.use { it.readLines().joinToString("\n") }
        ?: error("Missing bundled Flow Canvas CSS")
    val script = loader.getResourceAsStream("wecovi/ui/flow.js")?.bufferedReader()?.use { it.readLines().joinToString("\n") }
        ?: error("Missing bundled Flow Canvas JavaScript")
    return """<!doctype html><html><head><style>$css</style></head><body><div id="root"></div><script>$bridgeScript</script><script>$script</script></body></html>"""
}
