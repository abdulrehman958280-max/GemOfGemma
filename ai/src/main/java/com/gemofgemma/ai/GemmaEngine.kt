package com.gemofgemma.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.gemofgemma.ai.model.ModelDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** LiteRT-LM engine wrapper with persisted model selection and hot switching. */
@Singleton
class GemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager
) {
    private var engine: Engine? = null
    private var activeConversation: ManagedConversation? = null
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var isInitialized: Boolean = false
        private set
    @Volatile var initError: String? = null
        private set
    @Volatile var backendName: String = "CPU"
        private set

    init {
        scope.launch {
            modelDownloadManager.activeModelId.collect { modelId ->
                if (modelDownloadManager.isModelAvailable(modelId) && isInitialized) {
                    Log.i(TAG, "Active model changed to $modelId; reloading LiteRT-LM engine")
                    switchToModel(modelId)
                }
            }
        }
    }

    suspend fun initialize(modelPath: String, cacheDir: String) {
        withContext(Dispatchers.IO) { initializeInternal(modelPath, cacheDir) }
        if (!isInitialized) throw IllegalStateException(initError ?: "Engine initialization failed")
    }

    private fun initializeInternal(modelPath: String, cacheDir: String) {
        synchronized(lock) {
            try {
                val modelFile = File(modelPath)
                if (!modelFile.exists() || modelFile.length() == 0L) {
                    isInitialized = false
                    initError = "Model file not found or empty: $modelPath"
                    Log.e(TAG, initError!!)
                    return
                }

                releaseInternal()
                try {
                    @OptIn(ExperimentalApi::class)
                    run { ExperimentalFlags.enableSpeculativeDecoding = true }
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.GPU(),
                        visionBackend = Backend.CPU(),
                        cacheDir = cacheDir
                    )
                    engine = Engine(config).also { it.initialize() }
                    isInitialized = true
                    initError = null
                    backendName = "GPU"
                    Log.i(TAG, "GemmaEngine initialized with GPU backend")
                } catch (gpuEx: Exception) {
                    Log.w(TAG, "GPU init failed, falling back to CPU", gpuEx)
                    try {
                        val cpuConfig = EngineConfig(
                            modelPath = modelPath,
                            backend = Backend.CPU(),
                            visionBackend = Backend.CPU(),
                            cacheDir = cacheDir
                        )
                        engine = Engine(cpuConfig).also { it.initialize() }
                        isInitialized = true
                        initError = null
                        backendName = "CPU"
                        Log.i(TAG, "GemmaEngine initialized with CPU backend")
                    } catch (cpuEx: Exception) {
                        isInitialized = false
                        initError = "Both GPU and CPU initialization failed: ${cpuEx.message}"
                        Log.e(TAG, initError!!, cpuEx)
                    }
                }
            } catch (e: Exception) {
                isInitialized = false
                initError = "Engine initialization failed: ${e.message}"
                Log.e(TAG, initError!!, e)
            }
        }
    }

    private fun switchToModel(modelId: String) {
        scope.launch {
            try {
                initializeInternal(modelDownloadManager.getModelPath(modelId), context.cacheDir.path)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch to model $modelId", e)
            }
        }
    }

    fun getOrCreateConversation(conversationId: String, config: ConversationConfig, systemPrompt: String): ManagedConversation {
        synchronized(lock) {
            val existing = activeConversation
            if (existing != null && existing.id == conversationId) {
                if (existing.systemPrompt == systemPrompt) return existing
                return resetConversation(conversationId, config, systemPrompt)
            }
            closeActiveConversation()
            val eng = engine ?: throw IllegalStateException(initError ?: "Engine not initialized. Call initialize() first.")
            return ManagedConversation(eng.createConversation(config), conversationId, systemPrompt).also { activeConversation = it }
        }
    }

    fun resetConversation(conversationId: String, config: ConversationConfig, systemPrompt: String): ManagedConversation {
        synchronized(lock) {
            closeActiveConversation()
            val eng = engine ?: throw IllegalStateException(initError ?: "Engine not initialized. Call initialize() first.")
            return ManagedConversation(eng.createConversation(config), conversationId, systemPrompt).also { activeConversation = it }
        }
    }

    fun closeConversation(conversationId: String) {
        synchronized(lock) { if (activeConversation?.id == conversationId) closeActiveConversation() }
    }

    fun createOneShotConversation(config: ConversationConfig): Conversation {
        synchronized(lock) {
            closeActiveConversation()
            val eng = engine ?: throw IllegalStateException(initError ?: "Engine not initialized. Call initialize() first.")
            return eng.createConversation(config)
        }
    }

    private fun closeActiveConversation() {
        activeConversation?.let {
            try { it.conversation.close() } catch (e: Exception) { Log.w(TAG, "Error closing conversation", e) }
        }
        activeConversation = null
    }

    fun release() {
        synchronized(lock) { releaseInternal() }
    }

    private fun releaseInternal() {
        closeActiveConversation()
        try { engine?.close() } catch (e: Exception) { Log.w(TAG, "Error closing engine", e) }
        engine = null
        isInitialized = false
    }

    fun shutdown() {
        release()
        scope.cancel()
    }

    companion object { private const val TAG = "GemmaEngine" }
}

class ManagedConversation(
    val conversation: Conversation,
    val id: String,
    val systemPrompt: String
) {
    @Volatile var estimatedTokens: Int = 0
        private set
    @Volatile var turnCount: Int = 0
        private set

    fun recordExchange(userMessageChars: Int, responseChars: Int) {
        estimatedTokens += (userMessageChars + 3) / 4 + (responseChars + 3) / 4
        turnCount++
    }

    fun recordSystemPrompt(promptChars: Int) {
        estimatedTokens += (promptChars + 3) / 4
    }
}
