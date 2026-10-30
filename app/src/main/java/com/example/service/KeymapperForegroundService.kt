package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.ProfileRepository
import com.example.overlay.FloatingOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeymapperForegroundService : Service() {

    companion object {
        private const val TAG = "KeymapperFgs"
        const val CHANNEL_ID = "apex_mapper_service_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START = "com.example.apexmapper.action.START"
        const val ACTION_STOP = "com.example.apexmapper.action.STOP"
        const val ACTION_TOGGLE_PAUSE = "com.example.apexmapper.action.TOGGLE_PAUSE"
        const val ACTION_TOGGLE_MOUSE_LOCK = "com.example.apexmapper.action.TOGGLE_MOUSE_LOCK"
        const val ACTION_TOGGLE_OVERLAY = "com.example.apexmapper.action.TOGGLE_OVERLAY"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, KeymapperForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KeymapperForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlayManager: FloatingOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayManager = FloatingOverlayManager(this).also { it.start() }
        _isRunning.value = true
        Log.d(TAG, "KeymapperForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PAUSE -> {
                val current = KeymapperAccessibilityService.isMappingEnabled.value
                KeymapperAccessibilityService.setMappingEnabled(!current)
                updateNotification()
            }
            ACTION_TOGGLE_MOUSE_LOCK -> {
                KeymapperAccessibilityService.toggleMouseLock()
                updateNotification()
            }
            ACTION_TOGGLE_OVERLAY -> {
                overlayManager?.toggleOverlay()
            }
            else -> {
                startForegroundWithNotification()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Listen for state changes to update notification
        serviceScope.launch {
            KeymapperAccessibilityService.isMouseLocked.collectLatest {
                updateNotification()
            }
        }
        serviceScope.launch {
            KeymapperAccessibilityService.isMappingEnabled.collectLatest {
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val isPaused = !KeymapperAccessibilityService.isMappingEnabled.value
        val isMouseLocked = KeymapperAccessibilityService.isMouseLocked.value
        val profileName = ProfileRepository.getInstance(this).activeProfile.value.name

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, KeymapperForegroundService::class.java).apply { action = ACTION_TOGGLE_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mouseLockIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, KeymapperForegroundService::class.java).apply { action = ACTION_TOGGLE_MOUSE_LOCK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overlayIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, KeymapperForegroundService::class.java).apply { action = ACTION_TOGGLE_OVERLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, KeymapperForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when {
            isPaused -> "⏸️ Mapping Paused"
            isMouseLocked -> "🎯 Active | Mouse Locked (FPS View)"
            else -> "🎮 Active | Mouse Free"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ApexMapper: $profileName")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseIntent
            )
            .addAction(
                android.R.drawable.ic_menu_always_landscape_portrait,
                if (isMouseLocked) "Unlock Mouse" else "Lock Mouse",
                mouseLockIntent
            )
            .addAction(
                android.R.drawable.ic_menu_view,
                "Overlay",
                overlayIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Exit",
                stopIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ApexMapper Active Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps background keyboard & mouse input mapping active for FPS games"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager?.stop()
        overlayManager = null
        serviceScope.cancel()
        _isRunning.value = false
        Log.d(TAG, "KeymapperForegroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
