package com.example.audio

import android.net.Uri

/**
 * Contract for separating background music from speech.
 * A TFLite-backed implementation can be supplied without changing the UI or ViewModel.
 */
interface AudioProcessor {
    suspend fun process(
        input: Uri,
        musicBlockLevel: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): ProcessingResult
}

data class ProcessingResult(
    val input: Uri,
    val output: Uri? = null,
    val usedModel: Boolean = false,
    val message: String? = null,
)

/**
 * Safe baseline used until a packaged separation model is available.
 * It deliberately reports that no model was used instead of pretending that a
 * progress animation performed source separation.
 */
class ModelUnavailableAudioProcessor : AudioProcessor {
    override suspend fun process(
        input: Uri,
        musicBlockLevel: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): ProcessingResult {
        require(musicBlockLevel in 0f..1f) { "Music block level must be between 0 and 1" }
        onProgress(0)
        return ProcessingResult(
            input = input,
            usedModel = false,
            message = "لم يتم تضمين نموذج فصل الصوت بعد. أضف music_separator.tflite إلى assets لتفعيل المعالجة المحلية.",
        )
    }
}

/** A deterministic processor useful for previews and unit tests. */
class PreviewAudioProcessor : AudioProcessor {
    override suspend fun process(
        input: Uri,
        musicBlockLevel: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): ProcessingResult {
        require(musicBlockLevel in 0f..1f) { "Music block level must be between 0 and 1" }
        onProgress(100)
        return ProcessingResult(input = input, usedModel = true)
    }
}
