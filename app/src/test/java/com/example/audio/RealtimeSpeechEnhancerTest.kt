package com.example.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeSpeechEnhancerTest {
    @Test
    fun `processing preserves frame count and reports finite timing`() {
        val samples = ShortArray(4_800) { if (it % 2 == 0) 12_000 else -12_000 }
        val stats = RealtimeSpeechEnhancer(musicBlockLevel = 0.9f)
            .processInterleavedStereo(samples, samples.size)

        assertEquals(2_400, stats.frames)
        assertTrue(stats.processingMicros >= 0)
        assertTrue(stats.peak <= 1f)
    }

    @Test
    fun `high side energy is strongly reduced`() {
        val samples = ShortArray(4_800) { index -> if (index % 2 == 0) 16_000 else -16_000 }
        RealtimeSpeechEnhancer(musicBlockLevel = 0.9f).processInterleavedStereo(samples, samples.size)

        val outputPeak = samples.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("side signal should be attenuated", outputPeak < 5_000)
    }
}
