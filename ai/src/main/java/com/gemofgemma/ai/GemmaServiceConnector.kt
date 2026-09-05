package com.gemofgemma.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.gemofgemma.ai.model.ModelDownloadManager
import com.gemofgemma.core.AiModelInfo
import com.gemofgemma.core.AiProcessor
import com.gemofgemma.core.model.AiRequest
import com.gemofgemma.core.model.AiResponse
import com.gemofgemma.core.model.StreamChunk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaServiceConnector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager
) : AiProcessor {
    private val _service = MutableStateFlow<GemmaService?>(null)
    private val _isEngineReady = MutableStateFlow(false)
    override val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()
    private var serviceScope: CoroutineScope? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as GemmaService.GemmaBinder).getService()
            _service.value = service
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            serviceScope = scope
            scope.launch { service.isEngineReady.collect { _isEngineReady.value = it } }
            Log.i(TAG, "Bound to GemmaService")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _service.value = null
            _isEngineReady.value = false
            serviceScope?.cancel()
            serviceScope = null
        }
    }

    init {
        val intent = Intent(context, GemmaService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override suspend fun process(request: AiRequest): AiResponse {
        val service = withTimeoutOrNull(15_000L) { _service.filterNotNull().first() }
            ?: return AiResponse.ErrorResponse("AI service not available. Model may still be loading — please try again shortly.")
        return service.process(request)
    }

    override suspend fun processStreaming(request: AiRequest): Flow<StreamChunk> {
        return _service.value?.processStreaming(request)
            ?: throw IllegalStateException("AI service not available yet.")
    }

    override suspend fun postProcessChat(responseText: String): AiResponse {
        val service = withTimeoutOrNull(15_000L) { _service.filterNotNull().first() }
            ?: return AiResponse.ErrorResponse("AI service not available.")
        return service.routeChatResponse(responseText)
    }

    override suspend fun postProcessVisionChat(responseText: String, userMessage: String): AiResponse {
        val service = withTimeoutOrNull(15_000L) { _service.filterNotNull().first() }
            ?: return AiResponse.ErrorResponse("AI service not available.")
        return service.routeVisionChatResponse(responseText, userMessage)
    }

    override val isModelAvailable: StateFlow<Boolean>
        get() = modelDownloadManager.isModelAvailableFlow
    override val downloadProgress: StateFlow<Float>
        get() = modelDownloadManager.downloadProgress
    override val isDownloading: StateFlow<Boolean>
        get() = modelDownloadManager.isDownloading
    override val downloadedBytes: StateFlow<Long>
        get() = modelDownloadManager.downloadedBytes
    override val totalBytes: StateFlow<Long>
        get() = modelDownloadManager.totalBytes
    override val activeModelIdFlow: StateFlow<String>
        get() = modelDownloadManager.activeModelId

    override fun availableModels(): List<AiModelInfo> = modelDownloadManager.availableModels().map {
        AiModelInfo(
            id = it.id,
            name = it.name,
            description = it.description,
            expectedSizeBytes = it.expectedSizeBytes,
            minRamGb = it.minRamGb,
            supportsVision = it.supportsVision,
            supportsAudio = it.supportsAudio,
            recommended = it.recommended
        )
    }

    override fun activeModelId(): String = modelDownloadManager.activeModelId.value
    override fun isModelAvailable(modelId: String): Boolean = modelDownloadManager.isModelAvailable(modelId)
    override fun getModelSizeOnDisk(modelId: String): Long = modelDownloadManager.getModelSizeOnDisk(modelId)
    override fun hasPartialDownload(modelId: String): Boolean = modelDownloadManager.hasPartialDownload(modelId)

    override suspend fun downloadModel(): Result<Unit> = modelDownloadManager.downloadModel().map { }
    override suspend fun downloadModel(modelId: String): Result<Unit> = modelDownloadManager.downloadModel(modelId).map { }
    override suspend fun deleteModel(): Result<Unit> = modelDownloadManager.deleteModel()
    override suspend fun deleteModel(modelId: String): Result<Unit> = modelDownloadManager.deleteModel(modelId)
    override fun selectModel(modelId: String): Result<Unit> = modelDownloadManager.selectModel(modelId)

    override fun getModelSizeOnDisk(): Long = modelDownloadManager.getModelSizeOnDisk()
    override fun hasPartialDownload(): Boolean = modelDownloadManager.hasPartialDownload()

    override suspend fun resetChat(conversationId: String) {
        val service = withTimeoutOrNull(15_000L) { _service.filterNotNull().first() } ?: return
        service.resetChat(conversationId)
    }

    override suspend fun cancelGeneration() { /* Flow cancellation is cooperative. */ }

    companion object { private const val TAG = "GemmaServiceConnector" }
}
