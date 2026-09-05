package com.gemofgemma.core

import com.gemofgemma.core.model.AiRequest
import com.gemofgemma.core.model.AiResponse
import com.gemofgemma.core.model.StreamChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Abstraction over the on-device AI pipeline. */
interface AiProcessor {
    suspend fun process(request: AiRequest): AiResponse
    suspend fun processStreaming(request: AiRequest): Flow<StreamChunk>
    suspend fun postProcessChat(responseText: String): AiResponse
    suspend fun postProcessVisionChat(responseText: String, userMessage: String): AiResponse

    val isModelAvailable: StateFlow<Boolean>
    val isEngineReady: StateFlow<Boolean>
    val downloadProgress: StateFlow<Float>
    val isDownloading: StateFlow<Boolean>
    val downloadedBytes: StateFlow<Long>
    val totalBytes: StateFlow<Long>

    suspend fun downloadModel(): Result<Unit>
    suspend fun deleteModel(): Result<Unit>
    fun getModelSizeOnDisk(): Long
    fun hasPartialDownload(): Boolean

    /** Available curated LiteRT-LM models. Kept as opaque DTOs to the core layer. */
    fun availableModels(): List<AiModelInfo>
    fun activeModelId(): String
    fun isModelAvailable(modelId: String): Boolean
    fun getModelSizeOnDisk(modelId: String): Long
    fun hasPartialDownload(modelId: String): Boolean
    suspend fun downloadModel(modelId: String): Result<Unit>
    suspend fun deleteModel(modelId: String): Result<Unit>
    fun selectModel(modelId: String): Result<Unit>
    val activeModelIdFlow: StateFlow<String>

    suspend fun resetChat(conversationId: String)
    suspend fun cancelGeneration()
}

data class AiModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val expectedSizeBytes: Long,
    val minRamGb: Int,
    val supportsVision: Boolean,
    val supportsAudio: Boolean,
    val recommended: Boolean
)
