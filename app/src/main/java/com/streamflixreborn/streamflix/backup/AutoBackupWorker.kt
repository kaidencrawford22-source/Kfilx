package com.streamflixreborn.streamflix.backup

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.streamflixreborn.streamflix.R
import com.streamflixreborn.streamflix.database.AppDatabase
import com.streamflixreborn.streamflix.providers.Provider
import com.streamflixreborn.streamflix.providers.TmdbProvider

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val allProviders = Provider.providers.keys.toMutableList().apply {
                listOf("it", "en", "es", "de", "fr").forEach { lang ->
                    add(TmdbProvider(lang))
                }
            }

            val backupManager = BackupRestoreManager(
                applicationContext,
                allProviders.mapNotNull { provider ->
                    try {
                        val db = AppDatabase.getInstanceForProvider(provider.name, applicationContext)
                        ProviderBackupContext(
                            name = provider.name,
                            movieDao = db.movieDao(),
                            tvShowDao = db.tvShowDao(),
                            episodeDao = db.episodeDao(),
                            seasonDao = db.seasonDao(),
                            provider = provider
                        )
                    } catch (e: Exception) {
                        Log.w("AutoBackup", "Skipping ${provider.name}: ${e.message}")
                        null
                    }
                }
            )

            val result = backupManager.performAutoBackup()
            if (result != null) {
                Log.d("AutoBackup", "Auto-backup saved to $result")
                showNotification(true)
                Result.success()
            } else {
                showNotification(false)
                Result.failure()
            }
        } catch (t: Throwable) {
            Log.e("AutoBackup", "Auto-backup failed", t)
            showNotification(false)
            Result.failure()
        }
    }

    private fun showNotification(success: Boolean) {
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.let { PendingIntent.getActivity(applicationContext, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        val notification = NotificationCompat.Builder(applicationContext, BackupScheduler.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(applicationContext.getString(R.string.backup_auto_title))
            .setContentText(
                if (success) applicationContext.getString(R.string.backup_notification_success)
                else applicationContext.getString(R.string.backup_notification_fail)
            )
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        nm.notify(BackupScheduler.NOTIFICATION_ID, notification)
    }
}
