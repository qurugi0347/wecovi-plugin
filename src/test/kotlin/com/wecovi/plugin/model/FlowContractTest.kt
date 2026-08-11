package com.wecovi.plugin.model

import junit.framework.TestCase
import org.junit.Assert.assertThrows
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import kotlin.io.path.Path

class FlowContractTest : TestCase() {
    fun testGIVENValidFlowDocumentWHENSerializedTHENMatchesGoldenFixture() {
        // Given: a documented root with an undocumented expandable internal call and terminal boundaries.
        val document = basicFlowDocument()

        // When: the contract is serialized for a bridge consumer.
        val serialized = FlowJson.codec.parseToJsonElement(FlowJson.codec.encodeToString(document))

        // Then: its structure is the shared golden contract rather than a platform-specific payload.
        assertEquals(readGoldenFixture(), serialized)
        assertTrue(document.nodes.first().expandable)
        assertFalse(document.nodes.first().isDocumented)
    }

    fun testGIVENGoldenFixtureWHENDecodedAndReencodedTHENPreservesContract() {
        // Given: the JSON fixture shared with the future React consumer.
        val goldenFixture = readGoldenFixture()

        // When: it crosses the Kotlin contract boundary in both directions.
        val document = FlowJson.codec.decodeFromJsonElement<FlowDocument>(goldenFixture)
        val reencoded = FlowJson.codec.parseToJsonElement(FlowJson.codec.encodeToString(document))

        // Then: root metadata, node order, nested children, and offsets remain unchanged.
        assertEquals(goldenFixture, reencoded)
        assertEquals("src/user/signup.ts#signup", document.root.symbolId)
        assertEquals(3, document.nodes.size)
        assertEquals(1, document.nodes.first().children.size)
        assertEquals(82, document.nodes.first().sourceLocation.startOffset)
    }

    fun testGIVENContractEnumsWHENSerializedTHENUsesStableWireNames() {
        // Given: every node and terminal boundary kind reserved by the contract.
        val nodeKinds = mapOf(
            FlowNodeKind.CALL to "call",
            FlowNodeKind.CONSTRUCT to "construct",
            FlowNodeKind.AWAIT to "await",
            FlowNodeKind.RETURN to "return",
            FlowNodeKind.CONDITION to "condition",
            FlowNodeKind.SWITCH to "switch",
            FlowNodeKind.THROW to "throw",
            FlowNodeKind.TRY to "try",
            FlowNodeKind.CATCH to "catch",
            FlowNodeKind.LOOP to "loop",
            FlowNodeKind.PARALLEL to "parallel",
            FlowNodeKind.REFERENCE to "reference",
        )
        val boundaryKinds = mapOf(
            BoundaryKind.EXTERNAL to "external",
            BoundaryKind.UNRESOLVED to "unresolved",
            BoundaryKind.MULTIPLE to "multiple",
            BoundaryKind.RECURSIVE to "recursive",
            BoundaryKind.RUNTIME_BINDING to "runtimeBinding",
        )

        // When: each enum is written to JSON.
        val serializedNodeKinds = nodeKinds.keys.associateWith(FlowJson.codec::encodeToString)
        val serializedBoundaryKinds = boundaryKinds.keys.associateWith(FlowJson.codec::encodeToString)

        // Then: Kotlin enum constant names never leak into the wire format.
        nodeKinds.forEach { (kind, wireName) -> assertEquals("\"$wireName\"", serializedNodeKinds.getValue(kind)) }
        boundaryKinds.forEach { (kind, wireName) -> assertEquals("\"$wireName\"", serializedBoundaryKinds.getValue(kind)) }
    }

    fun testGIVENUnknownFieldOrInvalidStateWHENDecodedOrCreatedTHENRejectsContractDrift() {
        // Given: an unknown JSON field and invalid source/boundary states.
        val fixtureWithUnknownField = JsonObject(
            readGoldenFixture().jsonObject + ("unknownField" to JsonPrimitive(true)),
        )
        val invalidPaths = listOf("", "/private/source.ts", "C:/source.ts", "src/../source.ts", "src\\source.ts")

        // When: strict decoding or model construction is attempted.
        val unknownFieldFailure = assertThrows(SerializationException::class.java) {
            FlowJson.codec.decodeFromJsonElement<FlowDocument>(fixtureWithUnknownField)
        }

        // Then: non-contract JSON and unsafe source locations cannot reach consumers.
        assertNotNull(unknownFieldFailure)
        invalidPaths.forEach { path ->
            assertIllegalArgument { SourceLocation(path, startOffset = 0, endOffset = 0) }
        }
        assertIllegalArgument {
            basicNode(boundaryKind = BoundaryKind.EXTERNAL, expandable = true)
        }
    }

    private fun readGoldenFixture(): JsonElement {
        val projectDirectory = System.getProperty("wecovi.projectDir")
            ?: error("Gradle must provide the wecovi.projectDir test system property.")
        val fixturePath = Path(projectDirectory, "fixtures", "contracts", "basic-flow.json")

        return FlowJson.codec.parseToJsonElement(Files.readString(fixturePath))
    }

    private fun basicFlowDocument() = FlowDocument(
        root = FlowIndexEntry(
            symbolId = "src/user/signup.ts#signup",
            title = "회원가입",
            functionName = "signup",
            groupPath = listOf("User API", "User"),
            sourceLocation = SourceLocation("src/user/signup.ts", 0, 440),
            isRoot = true,
        ),
        nodes = listOf(
            basicNode(
                id = "src/user/signup.ts#signup:call:createUser",
                label = "사용자 생성",
                codeExpression = "await createUser(dto)",
                signature = "createUser(dto: SignupDto): Promise<User>",
                sourceLocation = SourceLocation("src/user/signup.ts", 82, 107),
                targetSymbolId = "src/user/create-user.ts#createUser",
                isDocumented = false,
                children = listOf(
                    basicNode(
                        id = "src/user/signup.ts#signup:call:createUser:return",
                        kind = FlowNodeKind.RETURN,
                        label = "생성한 사용자 반환",
                        codeExpression = "return user",
                        sourceLocation = SourceLocation("src/user/create-user.ts", 150, 161),
                        isDocumented = false,
                    ),
                ),
                expandable = true,
            ),
            basicNode(
                id = "src/user/signup.ts#signup:call:bcryptHash",
                label = "bcrypt.hash",
                codeExpression = "await bcrypt.hash(dto.password, 10)",
                signature = "hash(data: string, saltOrRounds: number): Promise<string>",
                sourceLocation = SourceLocation("src/user/signup.ts", 132, 167),
                boundaryKind = BoundaryKind.EXTERNAL,
                isDocumented = true,
            ),
            basicNode(
                id = "src/user/signup.ts#signup:call:notification",
                label = "알림 전송",
                codeExpression = "await notifier[channel](user)",
                sourceLocation = SourceLocation("src/user/signup.ts", 198, 228),
                boundaryKind = BoundaryKind.UNRESOLVED,
                isDocumented = false,
            ),
        ),
    )

    private fun basicNode(
        id: String = "node",
        kind: FlowNodeKind = FlowNodeKind.CALL,
        label: String = "node",
        codeExpression: String = "node()",
        signature: String? = null,
        sourceLocation: SourceLocation = SourceLocation("src/node.ts", 0, 1),
        targetSymbolId: String? = null,
        boundaryKind: BoundaryKind? = null,
        isDocumented: Boolean = true,
        children: List<FlowNode> = emptyList(),
        expandable: Boolean = false,
    ) = FlowNode(
        id = id,
        kind = kind,
        label = label,
        codeExpression = codeExpression,
        signature = signature,
        sourceLocation = sourceLocation,
        targetSymbolId = targetSymbolId,
        boundaryKind = boundaryKind,
        isDocumented = isDocumented,
        children = children,
        expandable = expandable,
    )

    private fun assertIllegalArgument(action: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java, action)
    }
}
