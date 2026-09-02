package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.audio.AudioProcessor
import com.example.audio.FfmpegAudioProcessor

class AudioCleanerViewModelFactory(
    context: Context,
    private val processor: AudioProcessor = FfmpegAudioProcessor(context.applicationContext),
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioCleanerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioCleanerViewModel(processor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
