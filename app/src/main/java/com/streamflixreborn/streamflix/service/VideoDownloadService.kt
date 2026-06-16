package com.streamflixreborn.streamflix.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.streamflixreborn.streamflix.R
import com.streamflixreborn.streamflix.utils.HlsDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class VideoDownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var downloadJob: Job? = null

    companion object {
        const val CHANNEL_ID = "video_downloads"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_URL = "extra_url"
        const val EXTRA_HEADERS = "extra_headers"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CONTENT_ID = "extra_content_id"

        private val activeDownloads = ConcurrentHashMap<String, Job>()

        fun isDownloading(contentId: String): Boolean {
            return activeDownloads.containsKey(contentId)
        }

        fun cancelDownload(contentId: String) {
            activeDownloads.remove(contentId)?.cancel()
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.download_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.download_channel_description)
                    setShowBadge(false)
                }
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Video"
        val contentId = intent.getStringExtra(EXTRA_CONTENT_ID) ?: url
        val headers = deserializeHeaders(intent)

        if (activeDownloads.containsKey(contentId)) return START_NOT_STICKY

        try {
            val notification = buildProgressNotification(title, 0, 0)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        downloadJob = scope.launch {
            activeDownloads[contentId] = downloadJob!!

            try {
                val downloader = HlsDownloader(
                    context = this@VideoDownloadService,
                    url = url,
                    headers = headers,
                    title = title,
                    onProgress = { done, total ->
                        updateProgressNotification(title, done, total)
                    }
                )
                val result = downloader.download()
                showCompleteNotification(title, result.uri, result.displayName)
            } catch (e: Exception) {
                showFailedNotification(title, e.message)
            } finally {
                activeDownloads.remove(contentId)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        downloadJob?.cancel()
        super.onDestroy()
    }

    private fun buildProgressNotification(title: String, done: Int, total: Int): Notification {
        val progress = if (total > 0) done * 100 / total else 0
        val text = if (total > 0) "$done / $total segments" else getString(R.string.download_starting)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.download_notif_title, title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, total == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateProgressNotification(title: String, done: Int, total: Int) {
        val notification = buildProgressNotification(title, done, total)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompleteNotification(title: String, uri: Uri, fileName: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.download_complete_title))
            .setContentText(getString(R.string.download_complete_text, title))
            .setSubText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailedNotification(title: String, error: String?) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.download_failed_title))
            .setContentText(getString(R.string.download_failed_text, title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(error ?: "Unknown error"))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeHeaders(intent: Intent): Map<String, String>? {
        @Suppress("DEPRECATION")
        val serialized = intent.getSerializableExtra(EXTRA_HEADERS)
        return serialized as? Map<String, String>
    }
}
