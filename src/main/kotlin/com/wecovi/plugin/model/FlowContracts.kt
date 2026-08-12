package com.wecovi.plugin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SourceLocation(
    val path: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    init {
        require(path.isNotBlank()) { "Source path must not be blank." }
        require(!path.startsWith('/') && !WINDOWS_DRIVE_PATH.matches(path)) {
            "Source path must be project-relative."
        }
        require(!path.contains('\\')) { "Source path must use '/' separators." }
        require(path.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
            "Source path must contain only non-relative segments."
        }
        require(startOffset >= 0) { "Source start offset must not be negative." }
        require(endOffset >= startOffset) { "Source end offset must not precede start offset." }
    }

    private companion object {
        val WINDOWS_DRIVE_PATH = Regex("^[A-Za-z]:.*")
    }
}

@Serializable
data class FlowIndexEntry(
    val symbolId: String,
    val title: String,
    val functionName: String,
    val groupPath: List<String>,
    val signature: String? = null,
    val sourceLocation: SourceLocation,
    val isRoot: Boolean,
)

@Serializable
data class FlowDocument(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val root: FlowIndexEntry,
    val nodes: List<FlowNode>,
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) {
            "Unsupported flow format version: $formatVersion"
        }
    }

    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class FlowNode(
    val id: String,
    val kind: FlowNodeKind,
    val label: String,
    val codeExpression: String,
    val signature: String? = null,
    val sourceLocation: SourceLocation,
    val targetSymbolId: String? = null,
    val boundaryKind: BoundaryKind? = null,
    val isDocumented: Boolean,
    val children: List<FlowNode> = emptyList(),
    val expandable: Boolean,
) {
    init {
        require(id.isNotBlank()) { "Flow node ID must not be blank." }
        require(boundaryKind == null || !expandable) {
            "A terminal boundary node must not be expandable."
        }
    }
}

@Serializable
enum class FlowNodeKind {
    @SerialName("call")
    CALL,

    @SerialName("construct")
    CONSTRUCT,

    @SerialName("await")
    AWAIT,

    @SerialName("return")
    RETURN,

    @SerialName("condition")
    CONDITION,

    @SerialName("switch")
    SWITCH,

    @SerialName("throw")
    THROW,

    @SerialName("try")
    TRY,

    @SerialName("catch")
    CATCH,

    @SerialName("loop")
    LOOP,

    @SerialName("parallel")
    PARALLEL,

    @SerialName("reference")
    REFERENCE,
}

@Serializable
enum class BoundaryKind {
    @SerialName("external")
    EXTERNAL,

    @SerialName("unresolved")
    UNRESOLVED,

    @SerialName("multiple")
    MULTIPLE,

    @SerialName("recursive")
    RECURSIVE,

    @SerialName("runtimeBinding")
    RUNTIME_BINDING,
}
