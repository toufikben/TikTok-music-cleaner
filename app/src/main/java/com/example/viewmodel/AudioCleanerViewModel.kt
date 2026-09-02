package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioProcessor
import com.example.audio.ModelUnavailableAudioProcessor
import com.example.audio.ProcessingResult
import com.example.model.VideoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data class Processing(val progress: Int) : ProcessingState()
    data class Success(val result: ProcessingResult) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class AudioCleanerViewModel(
    private val audioProcessor: AudioProcessor = ModelUnavailableAudioProcessor(),
) : ViewModel() {

    private var processingJob: Job? = null

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
        cancelProcessing()
        _selectedUri.value = Uri.parse(video.videoUrl)
        _processingState.value = ProcessingState.Idle
    }

    fun selectUri(uri: Uri) {
        cancelProcessing()
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
        _musicBlockLevel.value = level.coerceIn(0f, 1f)
    }

    fun setVocalBoost(enabled: Boolean) {
        _vocalBoostEnabled.value = enabled
    }

    fun setNoiseReduction(enabled: Boolean) {
        _noiseReductionEnabled.value = enabled
    }

    fun startProcessing() {
        val currentVideo = _selectedVideo.value ?: run {
            _processingState.value = ProcessingState.Error("اختر فيديو أولاً")
            return
        }
        val input = _selectedUri.value ?: Uri.parse(currentVideo.videoUrl)
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            try {
                val result = audioProcessor.process(
                    input = input,
                    musicBlockLevel = _musicBlockLevel.value,
                    vocalBoost = _vocalBoostEnabled.value,
                    noiseReduction = _noiseReductionEnabled.value,
                ) { progress ->
                    _processingState.value = ProcessingState.Processing(progress.coerceIn(0, 100))
                }
                if (result.usedModel) {
                    _processingState.value = ProcessingState.Success(result)
                    val processed = currentVideo.copy(isProcessed = true, musicBlockLevel = _musicBlockLevel.value)
                    _historyList.update { list ->
                        list.filterNot { it.id == processed.id }.let { listOf(processed) + it }
                    }
                } else {
                    _processingState.value = ProcessingState.Error(
                        result.message ?: "تعذر فصل الموسيقى"
                    )
                }
            } catch (error: Throwable) {
                _processingState.value = ProcessingState.Error(
                    error.message ?: "حدث خطأ أثناء معالجة الصوت"
                )
            }
        }
    }

    fun resetProcessing() {
        processingJob?.cancel()
        processingJob = null
        _processingState.value = ProcessingState.Idle
    }

    override fun onCleared() {
        processingJob?.cancel()
        super.onCleared()
    }
}
