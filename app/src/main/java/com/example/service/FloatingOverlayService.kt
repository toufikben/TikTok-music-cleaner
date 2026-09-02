package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
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

class FloatingOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        startAsForegroundService()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
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
                Toast.makeText(this@FloatingOverlayService, "اختر فيديو لمعالجة صوته", Toast.LENGTH_SHORT).show()
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
             sixtyDp(),
            sixtyDp(),
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = sixteenDp()
        }
        windowManager?.addView(button, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForegroundService() {
        val channelId = "floating_cleaner"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "زر تنظيف الصوت العائم",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("TikTok Audio Cleaner")
            .setContentText("زر تنظيف الصوت العائم نشط")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun sixteenDp() = (16 * resources.displayMetrics.density).toInt()
    private fun sixtyDp() = (60 * resources.displayMetrics.density).toInt()

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.action.START_FLOATING"
        const val ACTION_STOP = "com.example.action.STOP_FLOATING"
        const val ACTION_OPEN_PROCESSOR = "com.example.action.OPEN_PROCESSOR"
    }
}
