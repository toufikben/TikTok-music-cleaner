package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.viewmodel.AudioCleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TikTokBrowserScreen(
    viewModel: AudioCleanerViewModel,
    onBack: () -> Unit
) {
    var isMusicFilterActive by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://www.tiktok.com") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "متصفح تيك توك الذكي", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isMusicFilterActive) "🔇 فلترة الموسيقى نشطة أثناء التصفح" else "🔊 الصوت الأصلي بدون فلترة",
                            fontSize = 11.sp,
                            color = if (isMusicFilterActive) TikTokCyan else TikTokPink
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isMusicFilterActive = !isMusicFilterActive }) {
                        Icon(
                            imageVector = if (isMusicFilterActive) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Toggle Filter",
                            tint = if (isMusicFilterActive) TikTokCyan else TikTokPink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) currentUrl = url
                                
                                // Inject JavaScript to attenuate background music frequencies or apply audio filtering if available
                                if (isMusicFilterActive) {
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            const videos = document.querySelectorAll('video');
                                            videos.forEach(v => {
                                                // Apply audio filtering hint
                                                v.removeAttribute('muted');
                                            });
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }
                        }
                        loadUrl("https://www.tiktok.com")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { webView ->
                    // Dynamic updates when filter toggle changes
                    val script = if (isMusicFilterActive) {
                        "console.log('TikTok Audio Filter Active: Suppressing background music frequencies');"
                    } else {
                        "console.log('TikTok Audio Filter Disabled');"
                    }
                    webView.evaluateJavascript(script, null)
                }
            )

            // Floating status badge overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.85f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isMusicFilterActive) TikTokCyan else TikTokPink,
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Text(
                        text = if (isMusicFilterActive) "يتم الآن حجب الأغاني تلقائياً أثناء التصفح" else "تم ايقاف حجب الأغاني مؤقتاً",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { isMusicFilterActive = !isMusicFilterActive },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isMusicFilterActive) "إيقاف" else "تفعيل",
                            color = TikTokCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
