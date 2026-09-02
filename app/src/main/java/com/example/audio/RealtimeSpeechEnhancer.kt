package com.example.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Very low-latency DSP baseline for live playback capture.
 *
 * It is intentionally not presented as neural source separation: it favors the
 * center channel (where speech is commonly mixed), removes sub-voice rumble,
 * and applies a gentle adaptive speech gain. It processes one PCM frame in place
 * and keeps no unbounded queue, so latency cannot grow over time.
 */
class RealtimeSpeechEnhancer(
    private val sampleRate: Int = 48_000,
    musicBlockLevel: Float = 0.9f,
) {
    private val block = musicBlockLevel.coerceIn(0f, 1f)
    private var previousInput = 0f
    private var previousOutput = 0f
    private var envelope = 0f

    fun processInterleavedStereo(samples: ShortArray, length: Int): ProcessingStats {
        val count = min(length, samples.size - samples.size % 2)
        val started = System.nanoTime()
        var peak = 0f
        var index = 0
        while (index < count) {
            val left = samples[index] / 32768f
            val right = samples[index + 1] / 32768f
            val center = (left + right) * 0.5f
            val side = (left - right) * 0.5f

            // One-pole high-pass around 110 Hz: cheap and stable for speech.
            val filteredCenter = highPass(center)

            val level = abs(center)
            envelope += (level - envelope) * if (level > envelope) 0.08f else 0.01f
            val speechGain = 1f + min(0.35f, envelope * 2.5f)
            val centeredSpeech = filteredCenter * speechGain
            val sideGain = 1f - block
            val outputLeft = centeredSpeech + side * sideGain * 0.25f
            val outputRight = centeredSpeech - side * sideGain * 0.25f
            samples[index] = toPcm(outputLeft)
            samples[index + 1] = toPcm(outputRight)
            peak = max(peak, max(abs(outputLeft), abs(outputRight)))
            index += 2
        }
        return ProcessingStats(
            frames = count / 2,
            processingMicros = (System.nanoTime() - started) / 1_000,
            peak = peak,
        )
    }

    private fun highPass(input: Float): Float {
        // alpha chosen for a ~110 Hz corner at 48 kHz.
        val alpha = 0.9857f
        val output = alpha * (previousOutput + input - previousInput)
        previousInput = input
        previousOutput = output
        return output
    }

    private fun toPcm(value: Float): Short =
        (value.coerceIn(-1f, 1f) * 32767f).toInt().toShort()

    data class ProcessingStats(
        val frames: Int,
        val processingMicros: Long,
        val peak: Float,
    )
}

class LiveLatencyMeter(private val sampleRate: Int = 48_000) {
    private var processedFrames = 0L
    var lastProcessingMicros: Long = 0
        private set
    var maxProcessingMicros: Long = 0
        private set

    fun record(stats: RealtimeSpeechEnhancer.ProcessingStats) {
        processedFrames += stats.frames
        lastProcessingMicros = stats.processingMicros
        maxProcessingMicros = max(maxProcessingMicros, stats.processingMicros)
    }

    fun reset() {
        processedFrames = 0
        lastProcessingMicros = 0
        maxProcessingMicros = 0
    }

    fun processedAudioMillis(): Long = processedFrames * 1_000 / sampleRate
}
