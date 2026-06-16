package com.streamflixreborn.streamflix.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val WORK_NAME = "auto_backup"
    private const val TAG = "BackupScheduler"
    const val NOTIFICATION_CHANNEL_ID = "auto_backup"
    const val NOTIFICATION_ID = 1001

    fun getIntervalHours(interval: String): Long {
        return when (interval) {
            "weekly" -> 168L
            "monthly" -> 720L
            else -> 24L
        }
    }

    fun schedule(context: Context, interval: String = "daily") {
        val hours = getIntervalHours(interval)
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            hours, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun isScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WORK_NAME)
            .get()
        return workInfos.any { !it.state.isFinished }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(com.streamflixreborn.streamflix.R.string.backup_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(com.streamflixreborn.streamflix.R.string.backup_notification_channel)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
