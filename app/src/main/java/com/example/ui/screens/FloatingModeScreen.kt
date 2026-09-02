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
import androidx.core.content.ContextCompat
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.viewmodel.AudioCleanerViewModel
import com.example.service.FloatingOverlayService

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
                        text = "1. امنح إذن العرض فوق التطبيقات.\n2. شغّل زر العائم ليظهر اختصار تنظيف الصوت فوق التطبيقات.\n3. اضغط الزر العائم للعودة إلى التطبيق واختيار فيديو محفوظ أو مستورد لمعالجته.\n4. تُنشأ نسخة جديدة من الفيديو عند نجاح النموذج، ويبقى الملف الأصلي دون تعديل. لا يمكن للتطبيق اعتراض صوت TikTok تلقائياً في الخلفية دون دعم من النظام أو من التطبيق المصدر.",
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
                            onValueChange = {
                                filterIntensity = it
                                viewModel.setMusicBlockLevel(it)
                            },
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
                            onCheckedChange = {
                                vocalBoost = it
                                viewModel.setVocalBoost(it)
                            }
                        )
                    }
                }
            }

            // Action Start / Stop Floating Guard
            Button(
                onClick = {
                    if (hasOverlayPermission) {
                        if (isFloatingActive) {
                            context.stopService(Intent(context, FloatingOverlayService::class.java))
                        } else {
                            ContextCompat.startForegroundService(
                                context,
                                Intent(context, FloatingOverlayService::class.java).setAction(FloatingOverlayService.ACTION_START),
                            )
                        }
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
                    text = if (isFloatingActive) "إيقاف زر التنظيف العائم" else "تشغيل زر التنظيف العائم",
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
                                text = "زر التنظيف العائم نشط الآن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "سيظهر اختصار فوق التطبيقات. اضغطه لاختيار فيديو ومعالجته داخل التطبيق؛ لا يتم تعديل الفيديو الأصلي.",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
