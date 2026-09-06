package com.gemofgemma.core.model

/** Parsed function/tool call produced by the AI model. */
data class ParsedAction(
    val functionName: String,
    val parameters: Map<String, Any>,
    val rawOutput: String = ""
)
