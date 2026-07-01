package com.autotrans.android.feature.capture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import com.autotrans.android.domain.repository.CaptureRepository

/**
 * Foreground service that holds the [MediaProjection] lifecycle.
 *
 * Android requires a foreground service of type `mediaProjection` to be running
 * before [android.media.projection.MediaProjectionManager.getMediaProjection] is called.
 * This service satisfies that requirement.
 *
 * ## Lifecycle
 * - Started by the UI after the user grants the screen capture permission.
 * - Maintains the persistent notification required by Android.
 * - Stopped by calling [stopSelf] or by the user via the notification action.
 */
@AndroidEntryPoint
class ScreenCaptureService : Service() {

    @Inject
    lateinit var captureRepository: CaptureRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("ScreenCaptureService created")
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("ScreenCaptureService started")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        captureRepository.releaseResources()
        Timber.d("ScreenCaptureService destroyed")
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun buildNotification(): Notification {
        val channelId = ensureNotificationChannel()
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AutoTrans")
            .setContentText("Screen capture active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureNotificationChannel(): String {
        val channelId = "autotrans_capture"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while AutoTrans is capturing the screen"
            }
            nm.createNotificationChannel(channel)
        }
        return channelId
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun createStartIntent(context: Context): Intent =
            Intent(context, ScreenCaptureService::class.java)
    }
}
