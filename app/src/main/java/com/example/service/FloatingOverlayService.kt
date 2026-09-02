package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.audio.LiveLatencyMeter
import com.example.audio.RealtimeSpeechEnhancer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the floating control and, when MediaProjection consent is supplied, captures
 * other apps' playback audio with a bounded low-latency loop. Android only permits
 * capture; it does not permit replacing the source app's output directly.
 */
class FloatingOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var captureThread: Thread? = null
    private val captureRunning = AtomicBoolean(false)
    private var enhancer: RealtimeSpeechEnhancer? = null
    private var latencyMeter: LiveLatencyMeter? = null

    override fun onCreate() {
        super.onCreate()
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val projectionData = intent?.getParcelableExtraCompat<Intent>(EXTRA_PROJECTION_DATA)
        val projectionResult = intent?.getIntExtra(EXTRA_PROJECTION_RESULT, 0) ?: 0
        val musicBlockLevel = intent?.getFloatExtra(EXTRA_MUSIC_BLOCK_LEVEL, 0.9f) ?: 0.9f
        try {
            startAsForegroundService(projectionData != null)
            if (projectionData != null && projectionResult != 0) {
                startPlaybackCapture(projectionResult, projectionData, musicBlockLevel)
            }
        } catch (error: Exception) {
            Toast.makeText(this, "تعذر تشغيل التنظيف المباشر: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "اسمح بالعرض فوق التطبيقات أولاً", Toast.LENGTH_LONG).show()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val button = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setBackgroundResource(R.drawable.floating_button_background)
            contentDescription = getString(R.string.floating_button_description)
            setOnClickListener {
                startActivity(
                    Intent(this@FloatingOverlayService, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_OPEN_PROCESSOR, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                Toast.makeText(this@FloatingOverlayService, "التقاط الصوت المباشر نشط؛ اختر إعدادات التنظيف", Toast.LENGTH_SHORT).show()
            }
        }
        overlayView = button
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            sixtyDp(), sixtyDp(), windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = sixteenDp()
        }
        try {
            windowManager?.addView(button, params)
        } catch (error: Exception) {
            overlayView = null
            Toast.makeText(this, "تعذر إظهار الزر العائم: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun startPlaybackCapture(resultCode: Int, data: Intent, musicBlockLevel: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "التنظيف المباشر يحتاج Android 10 أو أحدث", Toast.LENGTH_LONG).show()
            return
        }
        stopPlaybackCapture()
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)
        val projection = mediaProjection ?: error("لم يتم منح إذن التقاط الصوت")
        val sampleRate = 48_000
        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
        val bufferSize = (minBuffer * 2).coerceAtLeast(sampleRate / 50 * 4)
        val outputBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            encoding,
        ).coerceAtLeast(sampleRate / 50 * 4)
        audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()
        val record = audioRecord ?: error("تعذر إنشاء مسجل صوت TikTok")
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(outputBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val track = audioTrack ?: error("تعذر إنشاء مخرج الصوت المباشر")
        enhancer = RealtimeSpeechEnhancer(sampleRate, musicBlockLevel)
        latencyMeter = LiveLatencyMeter(sampleRate)
        captureRunning.set(true)
        record.startRecording()
        track.play()
        captureThread = Thread {
            val buffer = ShortArray(bufferSize / 2)
            while (captureRunning.get()) {
                val read = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read < 0) break
                val stats = enhancer?.processInterleavedStereo(buffer, read)
                if (stats != null) {
                    latencyMeter?.record(stats)
                    // Never queue work faster than real time: dropping a frame is
                    // preferable to allowing latency to grow beyond the budget.
                    if (stats.processingMicros <= MAX_PROCESSING_MICROS) {
                        track.write(buffer, 0, read, AudioTrack.WRITE_BLOCKING)
                    }
                }
                lastCapturedFrames += read
            }
        }.also { it.name = "TikTokAudioCapture"; it.start() }
    }

    private fun stopPlaybackCapture() {
        captureRunning.set(false)
        captureThread?.interrupt()
        captureThread = null
        runCatching { audioRecord?.stop() }
        audioRecord?.release()
        audioRecord = null
        runCatching { audioTrack?.stop() }
        audioTrack?.release()
        audioTrack = null
        enhancer = null
        latencyMeter = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        stopPlaybackCapture()
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForegroundService(capturingPlayback: Boolean) {
        val channelId = "floating_cleaner"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "زر تنظيف الصوت العائم", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }).setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("TikTok Music Cleaner")
            .setContentText(if (capturingPlayback) "التقاط صوت TikTok نشط" else "زر التنظيف العائم نشط")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (capturingPlayback) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun sixteenDp() = (16 * resources.displayMetrics.density).toInt()
    private fun sixtyDp() = (60 * resources.displayMetrics.density).toInt()

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.action.START_FLOATING"
        const val ACTION_STOP = "com.example.action.STOP_FLOATING"
        const val EXTRA_PROJECTION_RESULT = "extra_projection_result"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_MUSIC_BLOCK_LEVEL = "extra_music_block_level"
        const val MAX_PROCESSING_MICROS = 20_000L
        @Volatile var lastCapturedFrames: Long = 0
    }
}

private inline fun <reified T : android.os.Parcelable> Intent.getParcelableExtraCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(key, T::class.java)
    else @Suppress("DEPRECATION") getParcelableExtra(key)
