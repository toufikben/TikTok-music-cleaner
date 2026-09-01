package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.viewmodel.AudioCleanerViewModel
import com.example.viewmodel.ProcessingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessorScreen(
    viewModel: AudioCleanerViewModel,
    onBack: () -> Unit
) {
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val musicBlockLevel by viewModel.musicBlockLevel.collectAsState()
    val vocalBoost by viewModel.vocalBoostEnabled.collectAsState()
    val noiseReduction by viewModel.noiseReductionEnabled.collectAsState()
    val processingState by viewModel.processingState.collectAsState()

    var isPreviewOriginal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("معالجة وفلترة صوت الفيديو") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Video Player Mock Preview Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewOriginal) Icons.Default.MusicNote else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isPreviewOriginal) TikTokPink else TikTokCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isPreviewOriginal) "تشغيل الصوت الأصلي (مع الموسيقى)" else "تشغيل الصوت المنقى (الموسيقى محجوبة 🔇)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = selectedVideo?.title ?: "فيديو تيك توك",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Toggle Preview Mode Button at bottom right
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clickable { isPreviewOriginal = !isPreviewOriginal },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.DarkGray.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = if (isPreviewOriginal) "🔊 معاينة الأصلي" else "🔇 معاينة المنقى",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "إعدادات حجب الأغاني",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // Music Block Level Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("شدة حجب الموسيقى", fontSize = 14.sp)
                            Text(
                                text = "${(musicBlockLevel * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = TikTokPink
                            )
                        }
                        Slider(
                            value = musicBlockLevel,
                            onValueChange = { viewModel.setMusicBlockLevel(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.testTag("music_block_slider")
                        )
                        Text(
                            text = "يتم خفض ترددات الأغاني والموسيقى الخلفية مع الحفاظ على ترددات الصوت البشري.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    // Toggles for Vocal Boost and Noise Reduction
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تضخيم ووضوح الصوت البشري", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("جعل الكلام والحوارات أكثر وضوحاً وثباتاً", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = vocalBoost,
                            onCheckedChange = { viewModel.setVocalBoost(it) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إزالة الضوضاء والتشويش", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("فلترة الأصوات المزعجة في الخلفية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = noiseReduction,
                            onCheckedChange = { viewModel.setNoiseReduction(it) }
                        )
                    }
                }
            }

            // Processing Status & Action Button
            when (val state = processingState) {
                is ProcessingState.Idle -> {
                    Button(
                        onClick = { viewModel.startProcessing() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_processing_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TikTokPink)
                    ) {
                        Icon(Icons.Default.MusicOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بدء حجب الموسيقى وعزل الصوت",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is ProcessingState.Processing -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "جاري معالجة الصوت وحجب الموسيقى... (${state.progress}%)",
                                fontWeight = FontWeight.Bold,
                                color = TikTokPink
                            )
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = TikTokPink
                            )
                            Text(
                                text = "الفيديو الأصلي لن يتأثر نهائياً، سيتم فقط تصفية المسار الصوتي.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is ProcessingState.Success -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = TikTokCyan.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TikTokCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = "تمت المعالجة بنجاح!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "تم حجب الأغاني والموسيقى بنسبة ${(musicBlockLevel * 100).toInt()}% مع الحفاظ على الأصوات بوضوح تام.",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { /* Export action */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("export_video_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TikTokCyan)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حفظ الفيديو المنقى في المعرض",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetProcessing() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("إعادة الضبط أو تعديل الإعدادات")
                        }
                    }
                }
                is ProcessingState.Error -> {
                    Text(
                        text = "خطأ: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
