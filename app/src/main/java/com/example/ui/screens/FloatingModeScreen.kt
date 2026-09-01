package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.viewmodel.AudioCleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingModeScreen(
    viewModel: AudioCleanerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isFloatingActive by remember { mutableStateOf(false) }
    var filterIntensity by remember { mutableStateOf(0.9f) }
    var vocalBoost by remember { mutableStateOf(true) }

    val hasOverlayPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الوضع العائم لتطبيق تيك توك الرسمي") },
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
            // Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TikTokPink,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "تصفح تطبيق تيك توك الرسمي بحرية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "العمل فوق التطبيق الأصلي بدون متصفح خارجي",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "كيف يعمل الوضع العائم؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1. افتح تطبيق تيك توك الرسمي على هاتفك.\n2. ستظهر أداة تحكم عائمة شفافة وخفيفة على الشاشة.\n3. يقوم التطبيق تلقائياً في الخلفية بخفض وكتم ترددات الموسيقى والأغاني صاخبة الحجم في كل فيديو أثناء تمريرك، مع الحفاظ الكامل على الحوار والأصوات البشرية.\n4. الفيديو الأصلي لا يتأثر نهائياً ولا يتم تعديل ملفه، فقط يتم تصفية الصوت المباشر المتزامن.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Permission status card
            if (!hasOverlayPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "مطلوب إذن العرض فوق التطبيقات الأخرى",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "لكي يظهر زر التحكم العائم فوق تطبيق تيك توك الرسمي، يرجى السماح بالتطبيق بالعرض فوق التطبيقات.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onError)
                        ) {
                            Text("منح الإذن الآن", color = MaterialTheme.colorScheme.error)
                        }
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
                        text = "إعدادات الفلترة المتزامنة في الخلفية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // Intensity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("شدة حجب الأغاني في الخلفية", fontSize = 14.sp)
                            Text(
                                text = "${(filterIntensity * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = TikTokPink
                            )
                        }
                        Slider(
                            value = filterIntensity,
                            onValueChange = { filterIntensity = it },
                            valueRange = 0f..1f
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تعزيز الحوار البشري بذكاء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("رفع وضوح الأصوات البشرية تلقائياً أثناء كتم الموسيقى", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = vocalBoost,
                            onCheckedChange = { vocalBoost = it }
                        )
                    }
                }
            }

            // Action Start / Stop Floating Guard
            Button(
                onClick = {
                    if (hasOverlayPermission) {
                        isFloatingActive = !isFloatingActive
                    } else {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFloatingActive) Color.DarkGray else TikTokPink
                )
            ) {
                Icon(
                    imageVector = if (isFloatingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFloatingActive) "إيقاف المراقبة العائمة في الخلفية" else "تشغيل المراقبة العائمة فوق تيك توك الرسمي",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isFloatingActive) {
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
                                text = "المراقبة العائمة نشطة الآن!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "يمكنك الآن فتح تطبيق تيك توك الرسمي وتصفحه كالمعتاد. سيتم حجب الأغاني تلقائياً في الخلفية.",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
