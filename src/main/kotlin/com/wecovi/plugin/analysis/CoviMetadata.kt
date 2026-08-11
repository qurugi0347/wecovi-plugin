package com.wecovi.plugin.analysis

import com.wecovi.plugin.model.FlowIndexEntry

data class CoviIndex(
    val flows: List<FlowIndexEntry>,
    val functions: List<FlowIndexEntry>,
)
