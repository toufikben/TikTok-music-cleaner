package com.example.audio

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * FFmpeg-based voice-focused processor.
 *
 * FFmpeg filters can attenuate music and improve speech clarity, but they do
 * not perform neural source separation. For true stem separation, replace
 * the filter graph with a model-backed processor while keeping this contract.
 */
class FfmpegAudioProcessor(
    private val context: Context,
) : AudioProcessor {
    override suspend fun process(
        input: Uri,
        musicBlockLevel: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): ProcessingResult = withContext(Dispatchers.IO) {
        require(musicBlockLevel in 0f..1f) { "Music block level must be between 0 and 1" }

        val workDir = File(context.cacheDir, "audio_cleaner").apply { mkdirs() }
        val inputFile = File(workDir, "input_${System.nanoTime()}.mp4")
        val outputFile = File(workDir, "cleaned_${System.nanoTime()}.mp4")

        try {
            context.contentResolver.openInputStream(input)?.use { source ->
                inputFile.outputStream().use { target -> source.copyTo(target) }
            } ?: error("تعذر فتح ملف الفيديو")
            onProgress(10)

            val filter = buildVoiceFocusedFilter(musicBlockLevel, vocalBoost, noiseReduction)
            val command = listOf(
                "-y",
                "-i", inputFile.absolutePath,
                "-map", "0:v:0?",
                "-map", "0:a:0?",
                "-c:v", "copy",
                "-af", filter,
                "-c:a", "aac",
                "-b:a", "160k",
                "-ar", "48000",
                "-ac", "2",
                "-movflags", "+faststart",
                outputFile.absolutePath,
            ).joinToString(" ") { shellQuote(it) }

            onProgress(20)
            val session = FFmpegKit.execute(command)
            onProgress(95)
            check(ReturnCode.isSuccess(session.returnCode)) {
                "FFmpeg failed with return code ${session.returnCode}: ${session.failStackTrace.orEmpty()}"
            }
            check(outputFile.exists() && outputFile.length() > 0) { "لم يتم إنشاء ملف الفيديو الناتج" }
            onProgress(100)
            ProcessingResult(
                input = input,
                output = Uri.fromFile(outputFile),
                completed = true,
                usedModel = false,
                message = "تم خفض الموسيقى وتحسين وضوح الكلام عبر FFmpeg",
            )
        } finally {
            inputFile.delete()
        }
    }

    private fun buildVoiceFocusedFilter(
        level: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
    ): String {
        val reduction = String.format(Locale.US, "%.2f", -6.0f * level)
        val parts = mutableListOf(
            "highpass=f=90",
            "lowpass=f=7500",
            "equalizer=f=250:g=${reduction}:width_type=o:width=1.2",
            "equalizer=f=4000:g=${reduction}:width_type=o:width=1.4",
        )
        if (noiseReduction) parts += "afftdn=nr=${String.format(Locale.US, "%.1f", 8.0f * level)}:nf=-40"
        if (vocalBoost) parts += "equalizer=f=1800:g=2.5:width_type=o:width=1.0"
        parts += "acompressor=threshold=0.08:ratio=3:attack=5:release=80"
        return parts.joinToString(",")
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
