package com.example.model

data class VideoItem(
    val id: String,
    val title: String,
    val author: String,
    val videoUrl: String,
    val thumbnailRes: Int,
    val duration: String,
    val hasMusic: Boolean = true,
    val isProcessed: Boolean = false,
    val musicBlockLevel: Float = 0.85f
)
