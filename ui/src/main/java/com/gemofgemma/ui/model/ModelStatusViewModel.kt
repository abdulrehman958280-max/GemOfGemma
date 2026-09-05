package com.gemofgemma.ui.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemofgemma.core.AiModelInfo
import com.gemofgemma.core.AiProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelStatusViewModel @Inject constructor(
    private val aiProcessor: AiProcessor,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val models: List<AiModelInfo> = aiProcessor.availableModels()
    val activeModelId: StateFlow<String> = aiProcessor.activeModelIdFlow
    val isModelAvailable: StateFlow<Boolean> = aiProcessor.isModelAvailable
    val downloadProgress: StateFlow<Float> = aiProcessor.downloadProgress
    val isDownloading: StateFlow<Boolean> = aiProcessor.isDownloading
    val downloadedBytes: StateFlow<Long> = aiProcessor.downloadedBytes
    val totalBytes: StateFlow<Long> = aiProcessor.totalBytes
    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    fun isInstalled(modelId: String): Boolean = aiProcessor.isModelAvailable(modelId)
    fun sizeOnDisk(modelId: String): Long = aiProcessor.getModelSizeOnDisk(modelId)
    fun hasPartialDownload(modelId: String): Boolean = aiProcessor.hasPartialDownload(modelId)

    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            _downloadError.value = null
            aiProcessor.downloadModel(modelId).onFailure { e ->
                _downloadError.value = e.message ?: "Download failed"
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            _downloadError.value = null
            aiProcessor.deleteModel(modelId).onFailure { e ->
                _downloadError.value = e.message ?: "Delete failed"
            }
        }
    }

    fun selectModel(modelId: String) {
        _downloadError.value = aiProcessor.selectModel(modelId).exceptionOrNull()?.message
    }

    fun downloadModel() = downloadModel(aiProcessor.activeModelId())
    fun deleteModel() = deleteModel(aiProcessor.activeModelId())
    fun getModelSizeOnDisk(): Long = aiProcessor.getModelSizeOnDisk()
    fun hasPartialDownload(): Boolean = aiProcessor.hasPartialDownload()

    fun getAvailableStorageBytes(): Long = StatFs(context.filesDir.path).availableBytes

    fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun clearError() { _downloadError.value = null }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
    else -> "%.0f KB".format(bytes / 1_024.0)
}
