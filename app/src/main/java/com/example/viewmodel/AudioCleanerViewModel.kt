package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Processing(val progress: Int) : ProcessingState()
    object Success : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class AudioCleanerViewModel : ViewModel() {

    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val _musicBlockLevel = MutableStateFlow(0.9f)
    val musicBlockLevel: StateFlow<Float> = _musicBlockLevel.asStateFlow()

    private val _vocalBoostEnabled = MutableStateFlow(true)
    val vocalBoostEnabled: StateFlow<Boolean> = _vocalBoostEnabled.asStateFlow()

    private val _noiseReductionEnabled = MutableStateFlow(true)
    val noiseReductionEnabled: StateFlow<Boolean> = _noiseReductionEnabled.asStateFlow()

    private val _historyList = MutableStateFlow<List<VideoItem>>(emptyList())
    val historyList: StateFlow<List<VideoItem>> = _historyList.asStateFlow()

    val sampleVideos = listOf(
        VideoItem(
            id = "1",
            title = "Cooking Pasta with Loud Background Pop Song 🎵",
            author = "@chef_tahani",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailRes = android.R.drawable.ic_menu_camera,
            duration = "0:45"
        ),
        VideoItem(
            id = "2",
            title = "Street Interview about Tech & AI with Remix Music 🎧",
            author = "@tech_voice",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            thumbnailRes = android.R.drawable.ic_menu_slideshow,
            duration = "1:12"
        ),
        VideoItem(
            id = "3",
            title = "Morning Motivation & Vlog with Heavy Synth Music 🎶",
            author = "@vlog_daily",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            thumbnailRes = android.R.drawable.ic_menu_gallery,
            duration = "0:58"
        )
    )

    fun selectVideo(video: VideoItem) {
        _selectedVideo.value = video
        _selectedUri.value = null
        _processingState.value = ProcessingState.Idle
    }

    fun selectUri(uri: Uri) {
        _selectedUri.value = uri
        _selectedVideo.value = VideoItem(
            id = "custom_${System.currentTimeMillis()}",
            title = "Custom TikTok Video (Imported)",
            author = "@user_device",
            videoUrl = uri.toString(),
            thumbnailRes = android.R.drawable.ic_menu_send,
            duration = "0:30"
        )
        _processingState.value = ProcessingState.Idle
    }

    fun setMusicBlockLevel(level: Float) {
        _musicBlockLevel.value = level
    }

    fun setVocalBoost(enabled: Boolean) {
        _vocalBoostEnabled.value = enabled
    }

    fun setNoiseReduction(enabled: Boolean) {
        _noiseReductionEnabled.value = enabled
    }

    fun startProcessing() {
        val currentVideo = _selectedVideo.value ?: return
        viewModelScope.launch {
            _processingState.value = ProcessingState.Processing(0)
            for (i in 1..10) {
                kotlinx.coroutines.delay(200)
                _processingState.value = ProcessingState.Processing(i * 10)
            }
            _processingState.value = ProcessingState.Success
            
            // Add to history
            val processed = currentVideo.copy(isProcessed = true, musicBlockLevel = _musicBlockLevel.value)
            if (!_historyList.value.any { it.id == processed.id }) {
                _historyList.update { listOf(processed) + it }
            }
        }
    }

    fun resetProcessing() {
        _processingState.value = ProcessingState.Idle
    }
}
