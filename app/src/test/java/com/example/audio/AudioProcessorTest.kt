package com.example.audio

import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioProcessorTest {
    private val input = Uri.parse("content://test/video.mp4")

    @Test
    fun `unavailable processor reports a clear non-success result`() = runTest {
        val progress = mutableListOf<Int>()
        val result = ModelUnavailableAudioProcessor().process(input, 0.8f, true, true) {
            progress += it
        }

        assertFalse(result.usedModel)
        assertTrue(result.message.orEmpty().contains("music_separator.tflite"))
        assertEquals(listOf(0), progress)
    }

    @Test
    fun `preview processor reports completion`() = runTest {
        val progress = mutableListOf<Int>()
        val result = PreviewAudioProcessor().process(input, 0.5f, false, false) {
            progress += it
        }

        assertTrue(result.usedModel)
        assertEquals(listOf(100), progress)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `processor rejects an invalid block level`() = runTest {
        PreviewAudioProcessor().process(input, 1.5f, true, true) { }
    }
}
