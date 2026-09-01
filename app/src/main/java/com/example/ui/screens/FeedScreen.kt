package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VideoItem
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.viewmodel.AudioCleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: AudioCleanerViewModel,
    onBack: () -> Unit,
    onSelectAndProcess: (VideoItem) -> Unit
) {
    val sampleVideos = viewModel.sampleVideos

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تغذية تيك توك المباشرة (فلترة الموسيقى)") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sampleVideos) { video ->
                FeedItemCard(video = video) {
                    onSelectAndProcess(video)
                }
            }
        }
    }
}

@Composable
fun FeedItemCard(video: VideoItem, onProcessClick: () -> Unit) {
    var isMusicBlocked by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Video placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isMusicBlocked) Icons.Default.Mic else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isMusicBlocked) TikTokCyan else TikTokPink,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (isMusicBlocked) "🔊 الموسيقى محجوبة (الأصوات فقط)" else "🎵 الموسيقى مفعلة (مع الأغنية)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Top overlay badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    containerColor = if (isMusicBlocked) TikTokCyan else TikTokPink
                ) {
                    Text(
                        text = if (isMusicBlocked) "مفلتر (بدون أغاني)" else "أصلي (مع موسيقى)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = { isMusicBlocked = !isMusicBlocked },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isMusicBlocked) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Toggle Music",
                        tint = Color.White
                    )
                }
            }

            // Bottom metadata and action
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = video.author,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Button(
                    onClick = onProcessClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TikTokPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ضبط إعدادات حجب الأغاني لهذا الفيديو")
                }
            }
        }
    }
}
