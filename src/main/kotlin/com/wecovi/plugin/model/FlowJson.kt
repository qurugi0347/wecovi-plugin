package com.wecovi.plugin.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
object FlowJson {
    val codec = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
        prettyPrint = true
    }
}
