package com.wecovi.plugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.jcef.JBCefJSQuery
import com.wecovi.plugin.model.FlowDocument
import com.wecovi.plugin.model.FlowJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

internal class FlowBridge(
    private val session: FlowEditorSession,
    private val send: (String) -> Unit,
    private val openSource: (String) -> Unit,
) {
    fun handle(payload: String): JBCefJSQuery.Response {
        if (payload.length > MAX_PAYLOAD) return error(null, "invalid", "Message is too large")
        val message = runCatching { FlowJson.codec.parseToJsonElement(payload) as? JsonObject }.getOrNull()
            ?: return error(null, "invalid", "Invalid message")
        val type = message["type"]?.jsonPrimitive?.content ?: return error(null, "invalid", "Missing type")
        val nodeId = message["nodeId"]?.jsonPrimitive?.content
        if (type !in setOf("ready", "expandNode", "openSource")) return error(nodeId, "invalid", "Unsupported message")
        return runCatching {
            ApplicationManager.getApplication().runReadAction {
                when (type) {
                    "ready" -> sendDocument(session.reload())
                    "expandNode" -> sendResult(requireNotNull(nodeId), session.expand(requireNotNull(nodeId)))
                    "openSource" -> openSource(requireNotNull(nodeId))
                    else -> Unit
                }
            }
            JBCefJSQuery.Response(null)
        }.getOrElse { error(nodeId, "stale", it.message ?: "Flow is no longer available") }
    }

    private fun sendDocument(document: FlowDocument) = send(message("document", FlowJson.codec.encodeToString(FlowDocument.serializer(), document)))

    private fun sendResult(nodeId: String, document: FlowDocument) = send(
        buildJsonObject {
            put("type", JsonPrimitive("result"))
            put("nodeId", JsonPrimitive(nodeId))
            put("document", FlowJson.codec.parseToJsonElement(FlowJson.codec.encodeToString(FlowDocument.serializer(), document)))
        }.toString(),
    )

    private fun error(nodeId: String?, code: String, text: String): JBCefJSQuery.Response {
        send(buildJsonObject {
            put("type", JsonPrimitive("error")); put("code", JsonPrimitive(code)); put("message", JsonPrimitive(text))
            nodeId?.let { put("nodeId", JsonPrimitive(it)) }
        }.toString())
        return JBCefJSQuery.Response(null)
    }

    private fun message(type: String, document: String) = "{\"type\":\"$type\",\"document\":$document}"

    companion object { private const val MAX_PAYLOAD = 8_192 }
}

internal fun base64Json(json: String): String = Base64.getEncoder().encodeToString(json.toByteArray())
