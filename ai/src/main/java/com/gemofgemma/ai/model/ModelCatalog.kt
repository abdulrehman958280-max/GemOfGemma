package com.gemofgemma.ai.model

/**
 * Curated models that use the LiteRT-LM artifact format and are suitable for
 * this Android app. Keep this list explicit rather than accepting arbitrary
 * Hugging Face files: a .litertlm extension alone does not guarantee that an
 * artifact is compatible with this app's runtime, multimodal pipeline, or
 * phone-action tooling.
 */
data class ModelSpec(
    val id: String,
    val name: String,
    val description: String,
    val repositoryUrl: String,
    val downloadUrl: String,
    val filename: String,
    val expectedSizeBytes: Long,
    val sha256: String,
    val minRamGb: Int,
    val supportsVision: Boolean,
    val supportsAudio: Boolean,
    val recommended: Boolean = false
)

object ModelCatalog {
    val models: List<ModelSpec> = listOf(
        ModelSpec(
            id = "gemma-4-e2b-it",
            name = "Gemma 4 E2B",
            description = "Best balance for Android: fast, multimodal, and the default model.",
            repositoryUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            filename = "gemma-4-E2B-it.litertlm",
            expectedSizeBytes = 2_770_000_000L,
            sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            minRamGb = 4,
            supportsVision = true,
            supportsAudio = true,
            recommended = true
        ),
        ModelSpec(
            id = "gemma-4-e4b-it",
            name = "Gemma 4 E4B",
            description = "Higher-capability Gemma 4 variant for devices with more memory.",
            repositoryUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm",
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
            filename = "gemma-4-E4B-it.litertlm",
            expectedSizeBytes = 3_660_000_000L,
            sha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
            minRamGb = 8,
            supportsVision = true,
            supportsAudio = true
        )
    )

    val default: ModelSpec = models.first { it.recommended }

    fun require(id: String): ModelSpec =
        models.firstOrNull { it.id == id }
            ?: error("Unknown model id: $id")
}
