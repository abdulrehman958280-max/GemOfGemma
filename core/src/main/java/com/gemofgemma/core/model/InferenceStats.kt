package com.gemofgemma.core.model

/**
 * Per-message inference performance statistics.
 * Populated at the end of streaming and attached to the final [StreamChunk].
 */
data class InferenceStats(
    /** Decode tokens per second (tokens emitted ÷ decode wall-clock). */
    val tokensPerSecond: Float,
    /** Milliseconds from request start to first streaming chunk. */
    val timeToFirstTokenMs: Long,
    /** Approximate token count (number of streaming chunks received). */
    val totalTokens: Int,
    /** Total wall-clock milliseconds from request start to last token. */
    val totalTimeMs: Long,
    /** "GPU" or "CPU" — whichever backend initialized successfully. */
    val backend: String
) {
    /** Compact display format for the stats chip. */
    fun formatDisplay(): String {
        val tps = "%.0f".format(tokensPerSecond)
        return "$tps tok/s · $backend · ${timeToFirstTokenMs}ms TTFT"
    }
}
