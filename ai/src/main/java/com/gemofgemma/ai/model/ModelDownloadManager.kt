package com.gemofgemma.ai.model

import android.content.Context
import android.os.StatFs
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns local model storage, resumable downloads, integrity verification and
 * the currently selected model. Each model gets its own directory so switching
 * models never overwrites another installed artifact.
 */
@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val modelDir = File(context.filesDir, "models").also { it.mkdirs() }

    private val _activeModelId = MutableStateFlow(readActiveModelId())
    val activeModelId: StateFlow<String> = _activeModelId

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _downloadedBytes = MutableStateFlow(0L)
    val downloadedBytes: StateFlow<Long> = _downloadedBytes

    private val _totalBytes = MutableStateFlow(ModelCatalog.default.expectedSizeBytes)
    val totalBytes: StateFlow<Long> = _totalBytes

    private val _isModelAvailable = MutableStateFlow(isModelAvailable(_activeModelId.value))
    val isModelAvailableFlow: StateFlow<Boolean> = _isModelAvailable

    fun availableModels(): List<ModelSpec> = ModelCatalog.models

    fun getModelSpec(id: String): ModelSpec = ModelCatalog.require(id)

    fun isModelAvailable(modelId: String): Boolean {
        val spec = ModelCatalog.require(modelId)
        val file = modelFile(spec)
        return file.exists() && file.length() > 0L
    }

    fun isActiveModelAvailable(): Boolean = isModelAvailable(_activeModelId.value)

    fun getModelPath(): String = getModelPath(_activeModelId.value)

    fun getModelPath(modelId: String): String = modelFile(ModelCatalog.require(modelId)).absolutePath

    fun getModelSizeOnDisk(): Long = getModelSizeOnDisk(_activeModelId.value)

    fun getModelSizeOnDisk(modelId: String): Long {
        val file = modelFile(ModelCatalog.require(modelId))
        return if (file.exists()) file.length() else 0L
    }

    fun hasPartialDownload(): Boolean = hasPartialDownload(_activeModelId.value)

    fun hasPartialDownload(modelId: String): Boolean {
        val spec = ModelCatalog.require(modelId)
        val temp = tempFile(spec)
        return temp.exists() && temp.length() > 0L
    }

    fun selectModel(modelId: String): Result<Unit> {
        return try {
            ModelCatalog.require(modelId)
            prefs.edit().putString(KEY_ACTIVE_MODEL, modelId).apply()
            _activeModelId.value = modelId
            _isModelAvailable.value = isModelAvailable(modelId)
            _downloadedBytes.value = getModelSizeOnDisk(modelId)
            _totalBytes.value = ModelCatalog.require(modelId).expectedSizeBytes
            _downloadProgress.value = if (_isModelAvailable.value) 1f else 0f
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadModel(): Result<File> = downloadModel(_activeModelId.value)

    suspend fun downloadModel(modelId: String): Result<File> = withContext(Dispatchers.IO) {
        val spec = try {
            ModelCatalog.require(modelId)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        if (_isDownloading.value) {
            return@withContext Result.failure(Exception("Another model download is already in progress"))
        }

        if (isModelAvailable(modelId)) {
            if (_activeModelId.value != modelId) selectModel(modelId)
            _isModelAvailable.value = true
            _downloadProgress.value = 1f
            return@withContext Result.success(modelFile(spec))
        }

        val requiredBytes = maxOf(spec.expectedSizeBytes, 64L * 1024L * 1024L)
        val availableBytes = StatFs(context.filesDir.path).availableBytes
        if (availableBytes < requiredBytes + 512L * 1024L * 1024L) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Not enough storage. Need about ${formatBytes(requiredBytes)} plus 512 MB free; " +
                        "only ${formatBytes(availableBytes)} is available."
                )
            )
        }

        _isDownloading.value = true
        _downloadProgress.value = 0f
        _downloadedBytes.value = 0L
        _totalBytes.value = spec.expectedSizeBytes

        val target = modelFile(spec)
        val temp = tempFile(spec)
        target.parentFile?.mkdirs()

        try {
            var existingBytes = if (temp.exists()) temp.length() else 0L
            val requestBuilder = Request.Builder().url(spec.downloadUrl)
            if (existingBytes > 0L) requestBuilder.header("Range", "bytes=$existingBytes-")

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
                }
                val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))

                val append = response.code == 206 && existingBytes > 0L
                if (!append) existingBytes = 0L

                val bodyLength = body.contentLength()
                val totalSize = if (append && bodyLength > 0L) existingBytes + bodyLength else maxOf(spec.expectedSizeBytes, bodyLength)
                _totalBytes.value = totalSize
                var bytesRead = existingBytes
                _downloadedBytes.value = bytesRead
                _downloadProgress.value = if (totalSize > 0L) bytesRead.toFloat() / totalSize else 0f

                body.byteStream().use { input ->
                    temp.outputStream().buffered(64 * 1024).use { output ->
                        if (append) {
                            // Re-open in append mode when the server honored Range.
                            output.close()
                            temp.outputStream().buffered(64 * 1024).use { appendOutput ->
                                // The first stream was opened only to keep resource handling uniform.
                                // Actual writes happen below.
                                var read: Int
                                val buffer = ByteArray(64 * 1024)
                                while (input.read(buffer).also { read = it } != -1) {
                                    appendOutput.write(buffer, 0, read)
                                    bytesRead += read
                                    _downloadedBytes.value = bytesRead
                                    _downloadProgress.value = if (totalSize > 0L) bytesRead.toFloat() / totalSize else 0f
                                }
                            }
                        } else {
                            var read: Int
                            val buffer = ByteArray(64 * 1024)
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                bytesRead += read
                                _downloadedBytes.value = bytesRead
                                _downloadProgress.value = if (totalSize > 0L) bytesRead.toFloat() / totalSize else 0f
                            }
                        }
                    }
                }
            }

            if (!temp.exists() || temp.length() <= 0L) {
                return@withContext Result.failure(Exception("Downloaded model is empty"))
            }

            if (temp.length() != spec.expectedSizeBytes && spec.expectedSizeBytes > 0L) {
                Log.w(TAG, "${spec.id}: expected about ${spec.expectedSizeBytes} bytes, received ${temp.length()} bytes; continuing to checksum")
            }

            val actualSha = sha256(temp)
            if (!actualSha.equals(spec.sha256, ignoreCase = true)) {
                temp.delete()
                return@withContext Result.failure(
                    SecurityException("Model integrity check failed. The downloaded file does not match the published SHA-256.")
                )
            }

            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                return@withContext Result.failure(Exception("Failed to finalize model file"))
            }

            if (_activeModelId.value != modelId) selectModel(modelId)
            _isModelAvailable.value = true
            _downloadedBytes.value = target.length()
            _totalBytes.value = target.length()
            _downloadProgress.value = 1f
            Result.success(target)
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed for ${spec.id}", e)
            Result.failure(e)
        } finally {
            _isDownloading.value = false
        }
    }

    suspend fun deleteModel(): Result<Unit> = deleteModel(_activeModelId.value)

    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val spec = ModelCatalog.require(modelId)
            tempFile(spec).delete()
            modelFile(spec).delete()
            if (_activeModelId.value == modelId) {
                _isModelAvailable.value = false
                _downloadProgress.value = 0f
                _downloadedBytes.value = 0L
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun modelFile(spec: ModelSpec): File = File(modelDir, spec.id).resolve(spec.filename)

    private fun tempFile(spec: ModelSpec): File = File(modelDir, spec.id).resolve("${spec.filename}.tmp")

    private fun readActiveModelId(): String {
        val stored = prefs.getString(KEY_ACTIVE_MODEL, null)
        return ModelCatalog.models.firstOrNull { it.id == stored }?.id ?: ModelCatalog.default.id
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val PREFS_NAME = "gemofgemma_models"
        private const val KEY_ACTIVE_MODEL = "active_model_id"

        private fun formatBytes(bytes: Long): String = when {
            bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
            else -> "%.0f KB".format(bytes / 1_024.0)
        }
    }
}
